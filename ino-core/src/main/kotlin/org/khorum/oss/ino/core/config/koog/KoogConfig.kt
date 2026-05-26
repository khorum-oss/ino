package org.khorum.oss.ino.core.config.koog

import ai.koog.http.client.ktor.KtorKoogHttpClient
import org.khorum.oss.ino.core.config.AgentConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires Koog plumbing as Spring beans so [AgentBridge] (and downstream
 * services that depend on it) can be autowired.
 *
 * Koog has a Spring Boot starter, but it requires Spring Boot 3 — we're on
 * 4.1.0-M1. Same pattern we used for Liquibase: define explicit beans here
 * instead of relying on autoconfig.
 */
@Configuration
class KoogConfig(
    private val agentOpenAiConfig: AgentConfig.OpenAi
) {

    @Bean
    fun koogHttpClientFactory(): KtorKoogHttpClient.Factory = KtorKoogHttpClient.Factory()

    @Bean
    fun agentBridge(httpClientFactory: KtorKoogHttpClient.Factory): AgentBridge =
        AgentBridge(
            agentOpenAiConfig = agentOpenAiConfig,
            httpClientFactory = httpClientFactory
        )
}
