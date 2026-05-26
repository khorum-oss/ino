package org.khorum.oss.ino.core.domain

import java.time.Instant

data class Message(
    val id: String,
    val sessionId: String,
    val seq: Int,
    val role: MessageRole,
    val content: String? = null,
    val reasoning: String? = null,
    val toolCallId: String? = null,
    val toolCalls: List<Map<String, Any?>>? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val createdAt: Instant,
)

enum class MessageRole(val dbValue: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    companion object {
        fun fromDb(value: String): MessageRole =
            entries.firstOrNull { it.dbValue == value }
                ?: error("Unknown MessageRole: $value")
    }
}
