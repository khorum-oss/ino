package org.khorum.oss.ino.core.persistence

import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.domain.ToolInvocation
import org.khorum.oss.ino.core.domain.ToolInvocationStatus
import org.springframework.jdbc.core.RowMapper
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.sql.ResultSet
import java.time.Instant

private fun ResultSet.getIntOrNull(column: String): Int? {
    val value = getInt(column)
    return if (wasNull()) null else value
}

private fun ResultSet.getLongOrNull(column: String): Long? {
    val value = getLong(column)
    return if (wasNull()) null else value
}

internal class SessionRowMapper(private val mapper: JsonMapper) : RowMapper<Session> {
    private val metadataType = object : TypeReference<Map<String, Any?>>() {}

    override fun mapRow(rs: ResultSet, rowNum: Int): Session = Session(
        id = rs.getString("id"),
        agentName = rs.getString("agent_name"),
        providerId = rs.getString("provider_id"),
        model = rs.getString("model"),
        status = SessionStatus.fromDb(rs.getString("status")),
        parentId = rs.getString("parent_id"),
        metadata = mapper.readValue(rs.getString("metadata_json"), metadataType),
        startedAt = Instant.parse(rs.getString("started_at")),
        endedAt = rs.getString("ended_at")?.let(Instant::parse),
        totalInputTokens = rs.getInt("total_input_tokens"),
        totalOutputTokens = rs.getInt("total_output_tokens"),
        totalCostUsdMicros = rs.getLong("total_cost_usd_micros"),
    )
}

internal class MessageRowMapper(private val mapper: JsonMapper) : RowMapper<Message> {
    private val toolCallsType = object : TypeReference<List<Map<String, Any?>>>() {}

    override fun mapRow(rs: ResultSet, rowNum: Int): Message = Message(
        id = rs.getString("id"),
        sessionId = rs.getString("session_id"),
        seq = rs.getInt("seq"),
        role = MessageRole.fromDb(rs.getString("role")),
        content = rs.getString("content"),
        reasoning = rs.getString("reasoning"),
        toolCallId = rs.getString("tool_call_id"),
        toolCalls = rs.getString("tool_calls_json")?.let { mapper.readValue(it, toolCallsType) },
        inputTokens = rs.getIntOrNull("input_tokens"),
        outputTokens = rs.getIntOrNull("output_tokens"),
        createdAt = Instant.parse(rs.getString("created_at")),
    )
}

internal object ToolInvocationRowMapper : RowMapper<ToolInvocation> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ToolInvocation = ToolInvocation(
        id = rs.getString("id"),
        messageId = rs.getString("message_id"),
        toolName = rs.getString("tool_name"),
        argumentsJson = rs.getString("arguments_json"),
        resultJson = rs.getString("result_json"),
        errorJson = rs.getString("error_json"),
        status = ToolInvocationStatus.fromDb(rs.getString("status")),
        startedAt = Instant.parse(rs.getString("started_at")),
        finishedAt = rs.getString("finished_at")?.let(Instant::parse),
        durationMs = rs.getLongOrNull("duration_ms"),
    )
}
