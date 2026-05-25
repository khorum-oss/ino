package org.khorum.oss.ino.core.domain

import java.time.Instant

data class ToolInvocation(
    val id: String,
    val messageId: String,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String? = null,
    val errorJson: String? = null,
    val status: ToolInvocationStatus,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val durationMs: Long? = null,
)

enum class ToolInvocationStatus(val dbValue: String) {
    PENDING("pending"),
    SUCCESS("success"),
    ERROR("error"),
    CANCELLED("cancelled");

    companion object {
        fun fromDb(value: String): ToolInvocationStatus =
            entries.firstOrNull { it.dbValue == value }
                ?: error("Unknown ToolInvocationStatus: $value")
    }
}
