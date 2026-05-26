package org.khorum.oss.ino.core.domain

import java.time.Instant

data class Session(
    val id: String,
    val agentName: String,
    val providerId: String,
    val model: String,
    val status: SessionStatus,
    val parentId: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalCostUsdMicros: Long = 0L,
)

enum class SessionStatus(val dbValue: String) {
    ACTIVE("active"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    companion object {
        fun fromDb(value: String): SessionStatus =
            entries.firstOrNull { it.dbValue == value }
                ?: error("Unknown SessionStatus: $value")
    }
}
