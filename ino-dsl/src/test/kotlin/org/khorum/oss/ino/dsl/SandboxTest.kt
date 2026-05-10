package org.khorum.oss.ino.dsl

import org.junit.jupiter.api.Test

class SandboxTest {
    @Test
    fun `minimum setup translates correctly`() {
        val agent = agent {
            name = "test-agent"
            provider {
                local {
                    model = "test-local"
                }
            }
        }

        val expected = Agent(
            name = "test-agent",
            provider = ProviderConfig(
                local = LocalConfig(
                    model = "test-local",
                )
            )
        )

        assert(expected == agent) {
            println("EXPECT: $expected\nACTUAL: $agent")
        }
    }

    @Test
    fun `full setup translates correctly with anthropic`() {
        val agent = setupTest {
            anthropic {
                model = "sonnet-4.6"
                apiKeyEnvVar = "ANTHROPIC_KEY"
                baseUrl = "https://anthropic.com"
                temperature = 1.0
                maxOutputTokens = 1000
                timeoutSeconds = 160
                anthropicVersion = "2026-01-01"
            }
        }

        val expected = createExpected(
            ProviderConfig(
                anthropic = AnthropicConfig(
                    model = "sonnet-4.6",
                    apiKeyEnvVar = "ANTHROPIC_KEY",
                    baseUrl = "https://anthropic.com",
                    temperature = 1.0,
                    maxOutputTokens = 1000,
                    timeoutSeconds = 160,
                    anthropicVersion = "2026-01-01"
                )
            )
        )

        assert(expected == agent) {
            println("EXPECT: $expected\nACTUAL: $agent")
        }
    }

    @Test
    fun `full setup translates correctly with openai`() {
        val agent = setupTest {
            openai {
                model = "gpt-5.5"
                apiKeyEnvVar = "OPENAI_KEY"
                baseUrl = "https://openai.com"
                temperature = 1.0
                maxOutputTokens = 1000
                timeoutSeconds = 160
                organization = "test-org"
                project = "test-project"
                parallelToolCalls()
            }
        }

        val expected = createExpected(
            ProviderConfig(
                openai = OpenAiConfig(
                    model = "gpt-5.5",
                    apiKeyEnvVar = "OPENAI_KEY",
                    baseUrl = "https://openai.com",
                    temperature = 1.0,
                    maxOutputTokens = 1000,
                    timeoutSeconds = 160,
                    organization = "test-org",
                    project = "test-project",
                    parallelToolCalls = true
                )
            )
        )

        assert(expected == agent) {
            println("EXPECT: $expected\nACTUAL: $agent")
        }
    }

    @Test
    fun `full setup translates correctly with local`() {
        val agent = setupTest {

            local {
                model = "test-local"
                host = "localhost"
                temperature = 1.0
                maxOutputTokens = 1000
                timeoutSeconds = 160
                keepAliveSeconds = 30
            }
        }

        val expected = createExpected(
            ProviderConfig(
                local = LocalConfig(
                    model = "test-local",
                    host = "localhost",
                    temperature = 1.0,
                    maxOutputTokens = 1000,
                    timeoutSeconds = 160,
                    keepAliveSeconds = 30
                )
            )
        )

        assert(expected == agent) {
            println("EXPECT: $expected\nACTUAL: $agent")
        }
    }

    @Test
    fun `full setup translates correctly with custom`() {
        val agent = setupTest {
            custom = CustomProvider(
                model = "test-custom"
            )
        }

        val expected = createExpected(
            ProviderConfig(
                custom = CustomProvider(model = "test-custom")
            )
        )

        assert(expected == agent) {
            println("EXPECT: $expected\nACTUAL: $agent")
        }
    }

    fun setupTest(providerBlock: ProviderConfigDslBuilder.() -> Unit): Agent = agent {
        name = "test-agent"
        description = "test description"
        systemPrompt = "You exist in a test state"
        maxIterations = 10
        budgetUsdMicros = 100

        provider {
            providerBlock()
        }

        tools {
            tool {
                name = "test-tool"
                description = "test-tool description"
                parameters {
                    toolParameter {
                        name = "test-parameter"
                        typeSpec = ParameterTypeSpec.StringSpec
                        description = "test-parameter description"
                        required()
                    }
                }

                version = "1.0.0"
                isNotDangerous()
                timeoutSeconds = 160
            }
        }
    }

    fun createExpected(providerConfig: ProviderConfig): Agent {
        return Agent(
            name = "test-agent",
            description = "test description",
            systemPrompt = "You exist in a test state",
            maxIterations = 10,
            budgetUsdMicros = 100,
            provider = providerConfig,
            tools = listOf(
                Tool(
                    name = "test-tool",
                    description = "test-tool description",
                    parameters = listOf(
                        ToolParameter(
                            name = "test-parameter",
                            typeSpec = ParameterTypeSpec.StringSpec,
                            description = "test-parameter description",
                            required = true
                        )
                    ),
                    version = "1.0.0",
                    dangerous = false,
                    timeoutSeconds = 160
                )
            )
        )
    }

    data class CustomProvider(
        override val model: String
    ) : LlmProviderConfig
}