package org.khorum.oss.ino.core.persistence

import org.khorum.oss.ino.core.domain.Message
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant

@Repository
class MessageRepository(
    private val jdbc: JdbcClient,
    private val mapper: JsonMapper,
    private val clock: Clock,
) {
    private val rowMapper = MessageRowMapper(mapper)

    /**
     * Appends a message and assigns the next sequence number for its session.
     *
     * The `(session_id, seq)` UNIQUE index protects against duplicate seqs,
     * so under concurrent writers the loser's INSERT fails fast — the caller
     * can retry, or wrap the call in the same SQLite write transaction as
     * other session mutations to serialize naturally (SQLite has one writer).
     */
    fun append(message: Message): Message {
        val nextSeq = jdbc.sql("SELECT COALESCE(MAX(seq), -1) + 1 FROM messages WHERE session_id = :sid")
            .param("sid", message.sessionId)
            .query(Int::class.java)
            .single()

        val effective = message.copy(
            seq = nextSeq,
            createdAt = message.createdAt.takeIf { it != Instant.EPOCH } ?: clock.instant(),
        )

        jdbc.sql(
            """
            INSERT INTO messages (
                id, session_id, seq, role, content, reasoning,
                tool_call_id, tool_calls_json, input_tokens, output_tokens, created_at
            ) VALUES (
                :id, :sessionId, :seq, :role, :content, :reasoning,
                :toolCallId, :toolCallsJson, :inputTokens, :outputTokens, :createdAt
            )
            """.trimIndent(),
        )
            .param("id", effective.id)
            .param("sessionId", effective.sessionId)
            .param("seq", effective.seq)
            .param("role", effective.role.dbValue)
            .param("content", effective.content)
            .param("reasoning", effective.reasoning)
            .param("toolCallId", effective.toolCallId)
            .param("toolCallsJson", effective.toolCalls?.let(mapper::writeValueAsString))
            .param("inputTokens", effective.inputTokens)
            .param("outputTokens", effective.outputTokens)
            .param("createdAt", effective.createdAt.toString())
            .update()

        return effective
    }

    fun listBySession(sessionId: String): List<Message> =
        jdbc.sql("SELECT * FROM messages WHERE session_id = :sid ORDER BY seq ASC")
            .param("sid", sessionId)
            .query(rowMapper)
            .list()

    fun countBySession(sessionId: String): Int =
        jdbc.sql("SELECT COUNT(*) FROM messages WHERE session_id = :sid")
            .param("sid", sessionId)
            .query(Int::class.java)
            .single()
}
