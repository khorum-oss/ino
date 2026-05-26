package org.khorum.oss.ino.core.koog

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.khorum.oss.ino.core.config.AgentConfig
import org.khorum.oss.ino.core.config.koog.AgentBridge
import org.khorum.oss.ino.dsl.AnthropicConfig
import org.khorum.oss.ino.dsl.LocalConfig
import org.khorum.oss.ino.dsl.OpenAiConfig
import org.khorum.oss.ino.dsl.agentDefinition
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure-translation tests for [org.khorum.oss.ino.core.config.koog.AgentBridge]. No network, no Spring context —
 * just verifies that DSL provider configs map to the right Koog `LLModel`
 * shape and that the bridge produces a usable `AIAgent`.
 *
 * Live wire tests against llama-server live in
 * [org.khorum.oss.ino.core.spike.KoogBridgeLlamaSmokeTest].
 */
class AgentBridgeTest {

    // AgentBridge needs the OpenAi config so its `baseUrl` default is wired in.
    // Production reads `ino.providers.openai.base-url`; tests construct inline.
    private val openAiConfig = AgentConfig.OpenAi(baseUrl = "https://api.openai.com")
    private val bridge = AgentBridge(openAiConfig)

    @Nested
    inner class ToLLModel {

        @Test
        fun `OpenAiConfig maps to OpenAI provider with Completions endpoint`() {
            val cfg = OpenAiConfig(model = "gpt-5", apiKeyEnvVar = "OPENAI_API_KEY")
            val model = with(bridge) { cfg.toLLModel() }

            assertEquals(LLMProvider.OpenAI, model.provider)
            assertEquals("gpt-5", model.id)
            val caps = assertNotNull(model.capabilities, "capabilities must be set")
            assertTrue(LLMCapability.Completion in caps, "expected Completion in $caps")
            assertTrue(LLMCapability.OpenAIEndpoint.Completions in caps, "expected OpenAIEndpoint.Completions in $caps")
        }

        @Test
        fun `LocalConfig maps to OpenAI provider with Completions endpoint`() {
            // LocalConfig routes through the same OpenAI-compatible client —
            // llama-server / vLLM / Ollama-shim all speak the same wire format.
            val cfg = LocalConfig(model = "qwen3-coder", host = "http://127.0.0.1:11435")
            val model = with(bridge) { cfg.toLLModel() }

            assertEquals(LLMProvider.OpenAI, model.provider)
            assertEquals("qwen3-coder", model.id)
            val caps = assertNotNull(model.capabilities)
            assertTrue(LLMCapability.OpenAIEndpoint.Completions in caps)
        }

        @Test
        fun `AnthropicConfig is not yet supported`() {
            val cfg = AnthropicConfig(model = "claude-opus-4-7")
            val ex = assertThrows<IllegalStateException> { with(bridge) { cfg.toLLModel() } }
            assertTrue(ex.message!!.contains("Anthropic"), "message should mention Anthropic: ${ex.message}")
        }
    }

    @Nested
    inner class ToKoogAgentDefinition {

        @Test
        fun `produces a runnable AIAgent for OpenAi config`() {
            val a = agentDefinition {
                name = "openai-test"
                provider {
                    openai {
                        model = "gpt-5"
                        baseUrl = "https://api.openai.com"
                    }
                }
                systemPrompt = "be brief"
            }
            val koogAgent = bridge.toKoogAgent(a)

            assertNotNull(koogAgent)
            assertNotNull(koogAgent.id, "Koog assigns an id to every agent")
            assertNotNull(koogAgent.agentConfig)
        }

        @Test
        fun `produces a runnable AIAgent for Local config`() {
            val a = agentDefinition {
                name = "local-test"
                provider {
                    local {
                        model = "qwen3-coder"
                        host = "http://127.0.0.1:11435"
                    }
                }
                systemPrompt = "be brief"
            }
            val koogAgent = bridge.toKoogAgent(a)

            assertNotNull(koogAgent)
            assertNotNull(koogAgent.id)
        }

        @Test
        fun `falls over for unsupported provider variants`() {
            val a = agentDefinition {
                name = "anthropic-test"
                provider {
                    anthropic { model = "claude-opus-4-7" }
                }
            }
            assertThrows<IllegalStateException> { bridge.toKoogAgent(a) }
        }
    }
}
