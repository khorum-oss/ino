package org.khorum.oss.ino.core.api.dto

import org.khorum.oss.ino.dsl.ParameterType
import org.khorum.oss.ino.dsl.ParameterTypeSpec
import org.khorum.oss.ino.dsl.Tool
import org.khorum.oss.ino.dsl.ToolParameter

/** Summary of a tool declaration as seen by an agent. */
data class ToolSummaryDto(
    val name: String,
    val description: String,
    val parameters: List<ToolParameterDto>,
    val dangerous: Boolean,
    val timeoutSeconds: Int?,
    val version: String?,
) {
    companion object {
        fun from(t: Tool): ToolSummaryDto = ToolSummaryDto(
            name = t.name,
            description = t.description,
            parameters = t.parameters.map { ToolParameterDto.from(it) },
            dangerous = t.dangerous,
            timeoutSeconds = t.timeoutSeconds,
            version = t.version,
        )
    }
}

/** Single tool parameter — type tag + structural shape, both shown. */
data class ToolParameterDto(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean,
) {
    companion object {
        fun from(p: ToolParameter): ToolParameterDto = ToolParameterDto(
            name = p.name,
            type = parameterTypeIdOf(p.typeSpec.type),
            description = p.description,
            required = p.required,
        )

        private fun parameterTypeIdOf(t: ParameterType): String = when (t) {
            ParameterType.STRING -> "string"
            ParameterType.NUMBER -> "number"
            ParameterType.INTEGER -> "integer"
            ParameterType.BOOLEAN -> "boolean"
            ParameterType.ARRAY -> "array"
            ParameterType.OBJECT -> "object"
        }
    }
}
