package org.khorum.oss.ino.core.agent

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.khorum.oss.ino.core.api.dto.MessageDto
import org.khorum.oss.ino.core.api.dto.SessionDto
import org.khorum.oss.ino.core.api.dto.StreamEventDto
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.koog.AgentBridge
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.khorum.oss.ino.dsl.Agent
import org.khorum.oss.ino.dsl.AnthropicConfig
import org.khorum.oss.ino.dsl.LlmProviderConfig
import org.khorum.oss.ino.dsl.LocalConfig
import org.khorum.oss.ino.dsl.OpenAiConfig
import org.springframework.stereotype.Component

/**
 * Orchestrates a single agent run end-to-end:
 *   1. Look up the DSL [Agent] in the registry.
 *   2. Persist the user message.
 *   3. Translate the DSL [Agent] via [AgentBridge] into a Koog `AIAgent`.
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
        val agentDef = registry.requireByName(agentName)
        val cfg = agentDef.provider.selected
        val session = store.startSession(
            agentName = agentDef.name,
            providerId = providerIdOf(cfg),
            model = cfg.model,
        )
        return SessionDto.from(session)
    }

    /**
     * Run the agent against a fresh user message. Synchronous (blocking on
     * the LLM call inside Koog) — used by the non-streaming endpoint. The
     * streaming counterpart will live in a separate method.
     */
    suspend fun sendUserMessage(sessionId: String, userContent: String): MessageDto {
        val session = store.findSession(sessionId)
            ?: throw NoSuchElementException("Session $sessionId not found")
        require(session.status == SessionStatus.ACTIVE) {
            "Session $sessionId is ${session.status}; cannot append a message"
        }

        val agentDef = registry.requireByName(session.agentName)

        store.appendUser(sessionId, userContent)

        val koogAgent = bridge.toKoogAgent(agentDef)
        val response = koogAgent.run(userContent)

        val assistantMsg = store.appendAssistant(sessionId, response)
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
        val session: Session = store.findSession(sessionId)
            ?: throw NoSuchElementException("Session $sessionId not found")
        require(session.status == SessionStatus.ACTIVE) {
            "Session $sessionId is ${session.status}; cannot append a message"
        }
        val agentDef = registry.requireByName(session.agentName)
        store.appendUser(sessionId, userContent)

        val components = bridge.componentsFor(agentDef)
        val koogPrompt = prompt("ino-session-$sessionId") {
            if (components.systemPrompt.isNotBlank()) system(components.systemPrompt)
            user(userContent)
        }

        val textAcc = StringBuilder()
        var inputTokens: Int? = null
        var outputTokens: Int? = null

        return flow {
            // Koog's PromptExecutor.executeStreaming returns kotlinx.coroutines.flow.Flow<StreamFrame>
            // at the Kotlin source level (it's exposed as JDK Flow.Publisher for Java callers).
            //
            // Note: we used to register the cancellation hook here, but it ran AFTER the first
            // frame was emitted. Moved to .onStart so DELETE /run works even before the first
            // text-delta arrives.
            components.executor.executeStreaming(koogPrompt, components.model).collect { frame ->
                when (frame) {
                    is StreamFrame.TextDelta -> {
                        textAcc.append(frame.text)
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
                        inputTokens = frame.metaInfo.inputTokensCount
                        outputTokens = frame.metaInfo.outputTokensCount
                        emit(
                            StreamEventDto.end(
                                stopReason = frame.finishReason,
                                inputTokens = inputTokens,
                                outputTokens = outputTokens,
                            ),
                        )
                    }
                    else -> { /* TextComplete / ReasoningComplete — no-op for now */ }
                }
            }
        }.onStart {
            // Register the collector's Job so `DELETE /api/sessions/{id}/run` can cancel it.
            val job = currentCoroutineContext()[Job]
            if (job != null) activeRuns.register(sessionId, job)
        }.onCompletion { cause ->
            activeRuns.deregister(sessionId)
            // Persist whatever we accumulated (cancelled streams still save partial output).
            val text = textAcc.toString().ifEmpty { null }
            if (text != null) {
                store.appendAssistant(
                    sessionId = sessionId,
                    content = text,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                )
            }
            if (cause != null) {
                // Last-ditch error signal to the consumer. WebFlux already closes the SSE channel
                // on cause; this is a best-effort heads-up.
                val msg = cause.message ?: cause::class.qualifiedName ?: "stream error"
                emit(StreamEventDto.error(msg))
            }
        }
    }

    /** Stable identifier for the provider variant, used in [SessionDto.providerId]. */
    private fun providerIdOf(cfg: LlmProviderConfig): String = when (cfg) {
        is OpenAiConfig -> "openai"
        is LocalConfig -> "local"
        is AnthropicConfig -> "anthropic"
        else -> "custom:${cfg::class.qualifiedName ?: "unknown"}"
    }
}
