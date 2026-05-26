package org.khorum.oss.ino.core.api.dto

/**
 * Wire shape for one SSE event from the agent stream.
 *
 * Serialized as `{ "type": "...", ...fields }`. We use a flat data class with
 * nullable fields rather than a sealed hierarchy to keep Jackson serialization
 * trivial — every event ships through the same controller path, and the
 * dashboard discriminates on `type`.
 *
 * Stable across providers — the SSE consumer doesn't care whether the
 * underlying stream came from llama-server, OpenAI, or Anthropic.
 */
data class StreamEventDto(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val argsJsonChunk: String? = null,
    val stopReason: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
) {
    companion object {
        const val TYPE_TEXT_DELTA = "text-delta"
        const val TYPE_REASONING_DELTA = "reasoning-delta"
        const val TYPE_TOOL_CALL_START = "tool-call-start"
        const val TYPE_TOOL_CALL_ARGS_DELTA = "tool-call-args-delta"
        const val TYPE_TOOL_CALL_END = "tool-call-end"
        const val TYPE_END = "end"
        const val TYPE_ERROR = "error"

        fun textDelta(text: String) = StreamEventDto(type = TYPE_TEXT_DELTA, text = text)
        fun reasoningDelta(text: String) = StreamEventDto(type = TYPE_REASONING_DELTA, text = text)
        fun toolCallStart(id: String, name: String) =
            StreamEventDto(type = TYPE_TOOL_CALL_START, id = id, name = name)

        fun toolCallArgsDelta(id: String, argsJsonChunk: String) =
            StreamEventDto(type = TYPE_TOOL_CALL_ARGS_DELTA, id = id, argsJsonChunk = argsJsonChunk)

        fun toolCallEnd(id: String) = StreamEventDto(type = TYPE_TOOL_CALL_END, id = id)

        fun end(stopReason: String? = null, inputTokens: Int? = null, outputTokens: Int? = null) =
            StreamEventDto(
                type = TYPE_END,
                stopReason = stopReason,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )

        fun error(text: String) = StreamEventDto(type = TYPE_ERROR, text = text)
    }
}
