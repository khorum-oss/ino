package org.khorum.oss.ino.core.api.dto

import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import java.time.Instant

/** Wire shape for a session. ISO-8601 timestamps serialized by Jackson. */
data class SessionDto(
    val id: String,
    val agentName: String,
    val providerId: String,
    val model: String,
    val status: String,
    val parentId: String?,
    val metadata: Map<String, Any?>,
    val startedAt: Instant,
    val endedAt: Instant?,
    val totalInputTokens: Int,
    val totalOutputTokens: Int,
    val totalCostUsdMicros: Long,
) {
    companion object {
        fun from(s: Session): SessionDto = SessionDto(
            id = s.id,
            agentName = s.agentName,
            providerId = s.providerId,
            model = s.model,
            status = s.status.dbValue,
            parentId = s.parentId,
            metadata = s.metadata,
            startedAt = s.startedAt,
            endedAt = s.endedAt,
            totalInputTokens = s.totalInputTokens,
            totalOutputTokens = s.totalOutputTokens,
            totalCostUsdMicros = s.totalCostUsdMicros,
        )
    }
}

@Suppress("UnusedReceiverParameter")
val SessionStatus.Companion.allValues: List<String>
    get() = SessionStatus.entries.map { it.dbValue }
