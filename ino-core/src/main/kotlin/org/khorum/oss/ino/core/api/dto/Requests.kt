package org.khorum.oss.ino.core.api.dto

/** Request body for `POST /api/sessions`. */
data class CreateSessionRequest(
    /** Agent name as registered in [org.khorum.oss.ino.core.agent.AgentRegistry]. */
    val agent: String,
)

/** Request body for `POST /api/sessions/{id}/messages`. */
data class AppendMessageRequest(
    val content: String,
)
