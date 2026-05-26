package org.khorum.oss.ino.core.persistence

import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant

@Repository
class SessionRepository(
    private val jdbc: JdbcClient,
    private val mapper: JsonMapper,
    private val clock: Clock,
) {
    private val rowMapper = SessionRowMapper(mapper)

    fun create(session: Session): Session {
        val effective = session.copy(
            status = session.status,
            startedAt = session.startedAt.takeIf { it != Instant.EPOCH } ?: clock.instant(),
        )
        jdbc.sql(
            """
            INSERT INTO sessions (
                id, agent_name, provider_id, model, status, parent_id, metadata_json,
                started_at, ended_at, total_input_tokens, total_output_tokens, total_cost_usd_micros
            ) VALUES (
                :id, :agentName, :providerId, :model, :status, :parentId, :metadataJson,
                :startedAt, :endedAt, :totalInputTokens, :totalOutputTokens, :totalCostUsdMicros
            )
            """.trimIndent(),
        )
            .param("id", effective.id)
            .param("agentName", effective.agentName)
            .param("providerId", effective.providerId)
            .param("model", effective.model)
            .param("status", effective.status.dbValue)
            .param("parentId", effective.parentId)
            .param("metadataJson", mapper.writeValueAsString(effective.metadata))
            .param("startedAt", effective.startedAt.toString())
            .param("endedAt", effective.endedAt?.toString())
            .param("totalInputTokens", effective.totalInputTokens)
            .param("totalOutputTokens", effective.totalOutputTokens)
            .param("totalCostUsdMicros", effective.totalCostUsdMicros)
            .update()
        return effective
    }

    fun findById(id: String): Session? = jdbc.sql("SELECT * FROM sessions WHERE id = :id")
        .param("id", id)
        .query(rowMapper)
        .optional()
        .orElse(null)

    fun listByAgent(agentName: String, limit: Int = DEFAULT_LIMIT, offset: Int = 0): List<Session> =
        jdbc.sql(
            """
            SELECT * FROM sessions
            WHERE agent_name = :agent
            ORDER BY started_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
        )
            .param("agent", agentName)
            .param("limit", limit)
            .param("offset", offset)
            .query(rowMapper)
            .list()

    fun updateStatus(id: String, status: SessionStatus, endedAt: Instant? = null) {
        jdbc.sql("UPDATE sessions SET status = :status, ended_at = :endedAt WHERE id = :id")
            .param("id", id)
            .param("status", status.dbValue)
            .param("endedAt", endedAt?.toString())
            .update()
    }

    fun bumpUsage(id: String, inputTokens: Int, outputTokens: Int, costMicros: Long) {
        jdbc.sql(
            """
            UPDATE sessions
            SET total_input_tokens    = total_input_tokens    + :inputTokens,
                total_output_tokens   = total_output_tokens   + :outputTokens,
                total_cost_usd_micros = total_cost_usd_micros + :costMicros
            WHERE id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("inputTokens", inputTokens)
            .param("outputTokens", outputTokens)
            .param("costMicros", costMicros)
            .update()
    }

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
