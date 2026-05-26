package org.khorum.oss.ino.core.persistence

import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.domain.ToolInvocation
import org.khorum.oss.ino.core.domain.ToolInvocationStatus
import org.khorum.oss.ino.core.util.UuidV7Generator
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * Facade over [SessionRepository], [MessageRepository], and
 * [ToolInvocationRepository]. The engine talks only to this type, so swapping
 * SQLite for Postgres later doesn't ripple through the executor.
 */
@Component
class ConversationStore(
    private val sessions: SessionRepository,
    private val messages: MessageRepository,
    private val toolInvocations: ToolInvocationRepository,
    private val ids: UuidV7Generator,
    private val clock: Clock,
) {

    fun startSession(
        agentName: String,
        providerId: String,
        model: String,
        metadata: Map<String, Any?> = emptyMap(),
        parentId: String? = null,
    ): Session = sessions.create(
        Session(
            id = ids.nextString(),
            agentName = agentName,
            providerId = providerId,
            model = model,
            status = SessionStatus.ACTIVE,
            parentId = parentId,
            metadata = metadata,
            startedAt = clock.instant(),
        ),
    )

    fun appendUser(sessionId: String, content: String): Message = messages.append(
        Message(
            id = ids.nextString(),
            sessionId = sessionId,
            seq = -1,
            role = MessageRole.USER,
            content = content,
            createdAt = clock.instant(),
        ),
    )

    fun appendAssistant(
        sessionId: String,
        content: String?,
        toolCalls: List<Map<String, Any?>>? = null,
        reasoning: String? = null,
        inputTokens: Int? = null,
        outputTokens: Int? = null,
    ): Message = messages.append(
        Message(
            id = ids.nextString(),
            sessionId = sessionId,
            seq = -1,
            role = MessageRole.ASSISTANT,
            content = content,
            reasoning = reasoning,
            toolCalls = toolCalls,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            createdAt = clock.instant(),
        ),
    )

    fun appendToolResult(sessionId: String, toolCallId: String, content: String): Message = messages.append(
        Message(
            id = ids.nextString(),
            sessionId = sessionId,
            seq = -1,
            role = MessageRole.TOOL,
            content = content,
            toolCallId = toolCallId,
            createdAt = clock.instant(),
        ),
    )

    fun recordToolInvocation(
        messageId: String,
        toolName: String,
        argumentsJson: String,
    ): ToolInvocation = toolInvocations.create(
        ToolInvocation(
            id = ids.nextString(),
            messageId = messageId,
            toolName = toolName,
            argumentsJson = argumentsJson,
            status = ToolInvocationStatus.PENDING,
            startedAt = clock.instant(),
        ),
    )

    fun finishToolInvocation(
        invocationId: String,
        status: ToolInvocationStatus,
        resultJson: String? = null,
        errorJson: String? = null,
        durationMs: Long,
    ) {
        toolInvocations.complete(
            id = invocationId,
            status = status,
            resultJson = resultJson,
            errorJson = errorJson,
            durationMs = durationMs,
        )
    }

    fun finalize(sessionId: String, status: SessionStatus) {
        sessions.updateStatus(sessionId, status, endedAt = clock.instant())
    }

    fun bumpUsage(sessionId: String, inputTokens: Int, outputTokens: Int, costMicros: Long) {
        sessions.bumpUsage(sessionId, inputTokens, outputTokens, costMicros)
    }

    fun messagesFor(sessionId: String): List<Message> = messages.listBySession(sessionId)

    fun invocationsFor(messageId: String): List<ToolInvocation> = toolInvocations.listByMessage(messageId)

    fun findSession(id: String): Session? = sessions.findById(id)

    fun listSessionsByAgent(agentName: String, limit: Int = DEFAULT_LIST_LIMIT, offset: Int = 0): List<Session> =
        sessions.listByAgent(agentName, limit, offset)

    companion object {
        const val DEFAULT_LIST_LIMIT: Int = 50
    }
}
