package org.khorum.oss.ino.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AgentConfig.Local::class,
    AgentConfig.OpenAi::class
)
class AgentConfig {
    @ConfigurationProperties(prefix = "ino.providers.local")
    class Local(
        val name: String,
        val description: String,
        val model: String,
        val baseUrl: String,
        val systemPrompt: String,
    )

    @ConfigurationProperties(prefix = "ino.providers.openai")
    class OpenAi(
        val baseUrl: String,
    )
}