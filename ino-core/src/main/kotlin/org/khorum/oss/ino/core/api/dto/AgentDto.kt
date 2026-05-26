package org.khorum.oss.ino.core.api.dto

import org.khorum.oss.ino.dsl.AgentDefinition
import org.khorum.oss.ino.dsl.AnthropicConfig
import org.khorum.oss.ino.dsl.LlmProviderConfig
import org.khorum.oss.ino.dsl.LocalConfig
import org.khorum.oss.ino.dsl.OpenAiConfig

/** Summary view of a registered agent — used by the listing endpoint. */
data class AgentSummaryDto(
    val name: String,
    val description: String,
    val providerId: String,
    val model: String,
) {
    companion object {
        fun from(a: AgentDefinition): AgentSummaryDto {
            val cfg = a.provider.selected
            return AgentSummaryDto(
                name = a.name,
                description = a.description,
                providerId = providerIdOf(cfg),
                model = cfg.model,
            )
        }
    }
}

/** Full view of an agent: same as the summary plus system prompt + tool list. */
data class AgentDetailsDto(
    val name: String,
    val description: String,
    val providerId: String,
    val model: String,
    val systemPrompt: String,
    val maxIterations: Int,
    val budgetUsdMicros: Long?,
    val tools: List<ToolSummaryDto>,
) {
    companion object {
        fun from(a: AgentDefinition): AgentDetailsDto {
            val cfg = a.provider.selected
            return AgentDetailsDto(
                name = a.name,
                description = a.description,
                providerId = providerIdOf(cfg),
                model = cfg.model,
                systemPrompt = a.systemPrompt,
                maxIterations = a.maxIterations,
                budgetUsdMicros = a.budgetUsdMicros,
                tools = a.tools.map { ToolSummaryDto.from(it) },
            )
        }
    }
}

private fun providerIdOf(cfg: LlmProviderConfig): String = when (cfg) {
    is OpenAiConfig -> "openai"
    is LocalConfig -> "local"
    is AnthropicConfig -> "anthropic"
    else -> "custom"
}
