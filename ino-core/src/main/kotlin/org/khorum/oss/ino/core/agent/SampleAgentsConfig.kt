package org.khorum.oss.ino.core.agent

import org.khorum.oss.ino.core.config.AgentConfig
import org.khorum.oss.ino.dsl.AgentDefinition
import org.khorum.oss.ino.dsl.agentDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Bootstrap set of agents available to the runtime. Eventually agents will
 * load from a filesystem / external registry; for the MVP, declaring them as
 * Spring `@Bean`s keeps the wiring obvious and DI-friendly.
 *
 * Add a new agent by:
 *  1. Defining it here as a top-level helper function returning [AgentDefinition].
 *  2. Adding the function reference to the `agentRegistry` bean below.
 */
@Configuration
class SampleAgentsConfig(
    private val agentLocalConfig: AgentConfig.Local,
) {

    @Bean
    fun agentRegistry(): AgentRegistry = AgentRegistry(
        listOf(
            llamaLocalAgent(),
        )
    )

    private fun llamaLocalAgent(): AgentDefinition = agentDefinition {
        name = agentLocalConfig.name
        description = agentLocalConfig.description
        provider {
            local {
                model = agentLocalConfig.model
                host = agentLocalConfig.baseUrl
            }
        }
        systemPrompt = agentLocalConfig.systemPrompt
    }
}
