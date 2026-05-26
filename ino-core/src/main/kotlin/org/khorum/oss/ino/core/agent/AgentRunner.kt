package org.khorum.oss.ino.core.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.khorum.oss.ino.core.api.dto.MessageDto
import org.khorum.oss.ino.core.api.dto.SessionDto
import org.khorum.oss.ino.core.api.dto.StreamEventDto
import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.config.koog.AgentBridge
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.khorum.oss.ino.dsl.AgentDefinition
import org.khorum.oss.ino.dsl.LlmProviderConfig
import org.springframework.stereotype.Component

/**
 * Orchestrates a single agent run end-to-end:
 *   1. Look up the DSL [AgentDefinition] in the registry.
 *   2. Persist the user message.
 *   3. Translate the DSL [AgentDefinition] via [AgentBridge] into a Koog `AIAgent`.
 *   4. Invoke `koogAgent.run(input)`.
 *   5. Persist the assistant message.
 *   6. Return a [MessageDto] for the API layer.
 *
 * Streaming (SSE) gets its own method in a follow-up — Koog exposes
 * `executeStreaming()` returning `Flow<StreamFrame>` which we'll convert into
 * Spring WebFlux `ServerSentEvent`s.
 */
@Component
class AgentRunner(
    private val registry: AgentRegistry,
    private val store: ConversationStore,
    private val bridge: AgentBridge,
    private val activeRuns: ActiveRunRegistry,
) {

    /** Create a session for [agentName] and return its DTO. */
    fun startSession(agentName: String): SessionDto {
        val agentDef: AgentDefinition = registry.requireByName(agentName)
        val cfg: LlmProviderConfig = agentDef.provider.selected
        val session: Session = store.startSession(
            agentName = agentDef.name,
            providerId = cfg.providerId(),
            model = cfg.model,
        )
        return SessionDto.from(session)
    }

    private fun findExistingNonActiveSession(sessionId: String): Session {
        val session = store.findSession(sessionId)
            ?: throw NoSuchElementException("Session $sessionId not found")

        require(session.status == SessionStatus.ACTIVE) {
            "Session $sessionId is ${session.status}; cannot append a message"
        }

        return session
    }

    private fun getAgentDefinitionBySession(sessionId: String): AgentDefinition {
        val session: Session = findExistingNonActiveSession(sessionId)
        return registry.requireByName(session.agentName)
    }

    /**
     * Run the agent against a fresh user message. Synchronous (blocking on
     * the LLM call inside Koog) — used by the non-streaming endpoint. The
     * streaming counterpart will live in a separate method.
     */
    suspend fun sendUserMessage(sessionId: String, userContent: String): MessageDto {
        val agentDef: AgentDefinition = getAgentDefinitionBySession(sessionId)

        store.appendUser(sessionId, userContent)

        val koogAgent: AIAgent<String, String> = bridge.toKoogAgent(agentDef)
        val response: String = koogAgent.run(userContent)

        val assistantMsg: Message = store.appendAssistant(sessionId, response)
        return MessageDto.from(assistantMsg)
    }

    /**
     * Stream Koog's per-token frames as SSE-ready [StreamEventDto]s.
     *
     * Mirrors [sendUserMessage] but emits incrementally. The assistant message
     * is persisted in `onCompletion` so that even cancelled streams record
     * whatever text accumulated.
     *
     * Conversation history is **not yet** included in the prompt — each call
     * sends just the system prompt + this user turn. Multi-turn history
     * threading is a follow-up (mark as @Enhancement: load prior messages
     * via `store.messagesFor(sessionId)` and inject as `user(...)/assistant(...)`
     * calls in the prompt DSL).
     */
    fun streamUserMessage(sessionId: String, userContent: String): Flow<StreamEventDto> {
        val agentDef = getAgentDefinitionBySession(sessionId)

        store.appendUser(sessionId, userContent)

        val components = bridge.componentsFor(agentDef)
        val koogPrompt = prompt("ino-session-$sessionId") {
            if (components.systemPrompt.isNotBlank()) system(components.systemPrompt)
            user(userContent)
        }

        val streamCapture = StreamCapture(sessionId)

        return flow {
            components.executor
                .executeStreaming(koogPrompt, components.model)
                .collect { frame -> handleFrame(streamCapture, frame) }
        }.onStart { registerJob(sessionId) }.onCompletion { cause -> storeLastEvent(streamCapture, cause) }
    }

    // Koog's PromptExecutor.executeStreaming returns kotlinx.coroutines.flow.Flow<StreamFrame>
    // at the Kotlin source level (it's exposed as JDK Flow.Publisher for Java callers).
    //
    // Note: we used to register the cancellation hook here, but it ran AFTER the first
    // frame was emitted. Moved to .onStart so DELETE /run works even before the first
    // text-delta arrives.
    private suspend fun FlowCollector<StreamEventDto>.handleFrame(streamCapture: StreamCapture, frame: StreamFrame) {
        when (frame) {
            is StreamFrame.TextDelta -> {
                streamCapture.textAcc.append(frame.text)
                emit(StreamEventDto.textDelta(frame.text))
            }
            is StreamFrame.ReasoningDelta -> {
                // ReasoningDelta.text is nullable in Koog — skip if absent.
                frame.text?.let { emit(StreamEventDto.reasoningDelta(it)) }
            }
            is StreamFrame.ToolCallDelta -> {
                // Koog emits one Delta per chunk. For MVP we surface each as args-delta;
                // the dashboard reassembles. Actual tool dispatch still needs registry wiring.
                emit(StreamEventDto.toolCallArgsDelta(frame.id.orEmpty(), frame.content.orEmpty()))
            }
            is StreamFrame.ToolCallComplete -> {
                emit(StreamEventDto.toolCallEnd(frame.id.orEmpty()))
            }
            is StreamFrame.End -> {
                streamCapture.inputTokens = frame.metaInfo.inputTokensCount
                streamCapture.outputTokens = frame.metaInfo.outputTokensCount
                emit(
                    StreamEventDto.end(
                        stopReason = frame.finishReason,
                        inputTokens = streamCapture.inputTokens,
                        outputTokens = streamCapture.outputTokens,
                    ),
                )
            }
            else -> { /* TextComplete / ReasoningComplete — no-op for now */ }
        }
    }

    // Register the collector's Job so `DELETE /api/sessions/{id}/run` can cancel it.
    private suspend fun registerJob(sessionId: String) {
        val job = currentCoroutineContext()[Job]
        if (job != null) activeRuns.register(sessionId, job)
    }

    private suspend fun FlowCollector<StreamEventDto>.storeLastEvent(
        streamCapture: StreamCapture,
        errorCause: Throwable?
    ) {
        activeRuns.deregister(streamCapture.sessionId)
        // Persist whatever we accumulated (cancelled streams still save partial output).
        val text = streamCapture.textAcc.toString().ifEmpty { null }
        if (text != null) {
            store.appendAssistant(
                sessionId = streamCapture.sessionId,
                content = text,
                inputTokens = streamCapture.inputTokens,
                outputTokens = streamCapture.outputTokens,
            )
        }
        if (errorCause != null) {
            // Last-ditch error signal to the consumer. WebFlux already closes the SSE channel
            // on cause; this is a best-effort heads-up.
            val msg = errorCause.message ?: errorCause::class.qualifiedName ?: "stream error"
            emit(StreamEventDto.error(msg))
        }
    }

    class StreamCapture(
        val sessionId: String,
        val textAcc: StringBuilder = StringBuilder(),
        var inputTokens: Int? = null,
        var outputTokens: Int? = null,
    )
}
