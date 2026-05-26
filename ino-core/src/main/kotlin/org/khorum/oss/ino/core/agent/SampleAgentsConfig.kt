package org.khorum.oss.ino.core.agent

import org.khorum.oss.ino.dsl.Agent
import org.khorum.oss.ino.dsl.agent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Bootstrap set of agents available to the runtime. Eventually agents will
 * load from a filesystem / external registry; for the MVP, declaring them as
 * Spring `@Bean`s keeps the wiring obvious and DI-friendly.
 *
 * Add a new agent by:
 *  1. Defining it here as a top-level helper function returning [Agent].
 *  2. Adding the function reference to the `agentRegistry` bean below.
 */
@Configuration
class SampleAgentsConfig(
    @Value($$"${app.llama-server.base-url:http://127.0.0.1:11435}")
    private val llamaServerBaseUrl: String,
) {

    @Bean
    fun agentRegistry(): AgentRegistry = AgentRegistry(
        listOf(
            llamaLocalAgent(),
        ),
    )

    private fun llamaLocalAgent(): Agent = agent {
        name = "llama-local"
        description = "Local llama.cpp server (qwen3-coder by default)."
        provider {
            local {
                model = "qwen3-coder"
                host = llamaServerBaseUrl
            }
        }
        systemPrompt = "You are a helpful, concise assistant. Reply in plain text."
    }
}
