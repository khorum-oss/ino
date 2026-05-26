package org.khorum.oss.ino.core.persistence

import org.khorum.oss.ino.core.domain.ToolInvocation
import org.khorum.oss.ino.core.domain.ToolInvocationStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.Instant

@Repository
class ToolInvocationRepository(
    private val jdbc: JdbcClient,
    private val clock: Clock,
) {

    fun create(invocation: ToolInvocation): ToolInvocation {
        val effective = invocation.copy(
            startedAt = invocation.startedAt.takeIf { it != Instant.EPOCH } ?: clock.instant(),
        )
        jdbc.sql(
            """
            INSERT INTO tool_invocations (
                id, message_id, tool_name, arguments_json, result_json, error_json,
                status, started_at, finished_at, duration_ms
            ) VALUES (
                :id, :messageId, :toolName, :argumentsJson, :resultJson, :errorJson,
                :status, :startedAt, :finishedAt, :durationMs
            )
            """.trimIndent(),
        )
            .param("id", effective.id)
            .param("messageId", effective.messageId)
            .param("toolName", effective.toolName)
            .param("argumentsJson", effective.argumentsJson)
            .param("resultJson", effective.resultJson)
            .param("errorJson", effective.errorJson)
            .param("status", effective.status.dbValue)
            .param("startedAt", effective.startedAt.toString())
            .param("finishedAt", effective.finishedAt?.toString())
            .param("durationMs", effective.durationMs)
            .update()
        return effective
    }

    fun complete(
        id: String,
        status: ToolInvocationStatus,
        resultJson: String? = null,
        errorJson: String? = null,
        finishedAt: Instant = clock.instant(),
        durationMs: Long,
    ) {
        jdbc.sql(
            """
            UPDATE tool_invocations
            SET status = :status,
                result_json = :resultJson,
                error_json = :errorJson,
                finished_at = :finishedAt,
                duration_ms = :durationMs
            WHERE id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .param("status", status.dbValue)
            .param("resultJson", resultJson)
            .param("errorJson", errorJson)
            .param("finishedAt", finishedAt.toString())
            .param("durationMs", durationMs)
            .update()
    }

    fun listByMessage(messageId: String): List<ToolInvocation> =
        jdbc.sql("SELECT * FROM tool_invocations WHERE message_id = :mid ORDER BY started_at ASC")
            .param("mid", messageId)
            .query(ToolInvocationRowMapper)
            .list()

    fun findById(id: String): ToolInvocation? =
        jdbc.sql("SELECT * FROM tool_invocations WHERE id = :id")
            .param("id", id)
            .query(ToolInvocationRowMapper)
            .optional()
            .orElse(null)
}
