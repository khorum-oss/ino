package org.khorum.oss.ino.core.api.dto

import org.khorum.oss.ino.core.domain.Message
import java.time.Instant

/** Wire shape for a single conversation message. */
data class MessageDto(
    val id: String,
    val sessionId: String,
    val seq: Int,
    val role: String,
    val content: String?,
    val reasoning: String?,
    val toolCallId: String?,
    val toolCalls: List<Map<String, Any?>>?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val createdAt: Instant,
) {
    companion object {
        fun from(m: Message): MessageDto = MessageDto(
            id = m.id,
            sessionId = m.sessionId,
            seq = m.seq,
            role = m.role.dbValue,
            content = m.content,
            reasoning = m.reasoning,
            toolCallId = m.toolCallId,
            toolCalls = m.toolCalls,
            inputTokens = m.inputTokens,
            outputTokens = m.outputTokens,
            createdAt = m.createdAt,
        )
    }
}
