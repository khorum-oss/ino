package org.khorum.oss.ino.dsl

import org.khorum.oss.konstellation.metaDsl.annotation.GeneratedDsl
import org.khorum.oss.konstellation.metaDsl.annotation.RootDsl
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyString

/** Conversation roles supported by the protocol. */
enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

/**
 * A single conversation message. Used both as a DSL-buildable seed (e.g. system prompts
 * declared in agent definitions) and as the read model the engine returns from sessions.
 *
 * Note: `toolCallId` and `toolCallsJson` are the on-the-wire fields needed when the role
 * is TOOL or ASSISTANT (with tool calls). They're optional here because most messages
 * declared via DSL won't use them — they're populated by the engine, not by users.
 */
@RootDsl
@GeneratedDsl
data class Message(
    val role: Role,
    @DefaultEmptyString
    val content: String = "",
    val toolCallId: String? = null,
    val toolCallsJson: String? = null,
    val reasoning: String? = null,
)
