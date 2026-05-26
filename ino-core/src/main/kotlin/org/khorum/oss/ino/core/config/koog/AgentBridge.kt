package org.khorum.oss.ino.core.config.koog

import ai.koog.agents.core.agent.AIAgent
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import org.khorum.oss.ino.core.config.AgentConfig
import org.khorum.oss.ino.dsl.AgentDefinition
import org.khorum.oss.ino.dsl.AnthropicConfig
import org.khorum.oss.ino.dsl.LlmProviderConfig
import org.khorum.oss.ino.dsl.LocalConfig
import org.khorum.oss.ino.dsl.OpenAiConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Bridge from konstellation DSL agent definitions to Koog's `AIAgent`.
 *
 * The DSL declares *what* an agent is (name, provider config, system prompt,
 * tools, iteration cap). Koog provides the runtime (LLM client, agent loop,
 * tool dispatch). This class is the glue.
 *
 * Coverage today:
 *  - `OpenAiConfig`  — cloud OpenAI or any compatible endpoint (baseUrl override).
 *  - `LocalConfig`   — local llama.cpp / llama-server / vLLM / Ollama (via its
 *                       OpenAI-compatible shim). Routed through the same
 *                       `OpenAILLMClient` with `host` as the baseUrl.
 *  - `AnthropicConfig` — TODO (see when-branch comment).
 *  - `custom`        — TODO (registry lookup by class).
 *
 * Stateless: callers may reuse a single instance or register it as a Spring
 * `@Component`.
 */
class AgentBridge(
    private val agentOpenAiConfig: AgentConfig.OpenAi,
    /**
     * Koog's HTTP client factory. Default matches the sandbox; inject a
     * custom factory in tests or to customize pooling / timeouts.
     */
    private val httpClientFactory: KtorKoogHttpClient.Factory = KtorKoogHttpClient.Factory(),
) {

    /** Translate a DSL `Agent` into a runnable Koog agent. */
    fun toKoogAgent(agentDefinition: AgentDefinition): AIAgent<String, String> {
        val bridgeComponents = componentsFor(agentDefinition)
        return AIAgent(
            promptExecutor = bridgeComponents.executor,
            llmModel = bridgeComponents.model,
            systemPrompt = bridgeComponents.systemPrompt,
        )
    }

    /**
     * Lower-level translation: just the Koog primitives, without wrapping them
     * in an `AIAgent`. Used by the streaming path, which talks to the
     * `PromptExecutor` directly to get a `Flow<StreamFrame>`.
     */
    fun componentsFor(agentDefinition: AgentDefinition): BridgeComponents {
        val cfg: LlmProviderConfig = agentDefinition.provider.selected
        return BridgeComponents(
            executor = cfg.toPromptExecutor(),
            model = cfg.toLLModel(),
            systemPrompt = agentDefinition.systemPrompt,
        )
    }

    /** Resolved Koog runtime pieces for a single DSL agent. */
    data class BridgeComponents(
        val executor: PromptExecutor,
        val model: LLModel,
        val systemPrompt: String,
    )

    /**
     * Translate a provider config into a Koog `PromptExecutor`.
     *
     * Both `OpenAiConfig` and `LocalConfig` route through `OpenAILLMClient` —
     * llama-server, vLLM, and Ollama's OpenAI shim all speak the same wire
     * format. The only difference is whether an API key is read from env.
     */
    fun LlmProviderConfig.toPromptExecutor(): PromptExecutor = when (this) {
        is OpenAiConfig -> openAiCompatibleExecutor(
            apiKey = readApiKey(apiKeyEnvVar),
            baseUrl = baseUrl ?: agentOpenAiConfig.baseUrl,
        )

        is LocalConfig -> openAiCompatibleExecutor(
            apiKey = "",            // local runtimes don't require a key
            baseUrl = host,
        )

        // TODO(@Enhancement): wire AnthropicLLMClient with apiKey + baseUrl;
        // needs the Koog Anthropic client capabilities list.
        is AnthropicConfig -> error("AnthropicConfig → Koog bridge not implemented yet")

        else -> error("Unknown LlmProviderConfig: ${this::class.qualifiedName}")
    }

    /**
     * Translate a provider config into a Koog `LLModel` (provider tag,
     * model id, and capabilities the engine should attempt to use).
     *
     * For OpenAI-compatible endpoints we declare:
     *   - `Completion` — classic chat completion.
     *   - `OpenAIEndpoint.Completions` — routes to `/v1/chat/completions`
     *     instead of the new Responses API (llama-server only speaks the
     *     legacy path).
     */
    fun LlmProviderConfig.toLLModel(): LLModel = when (this) {
        is OpenAiConfig, is LocalConfig -> LLModel(
            provider = LLMProvider.OpenAI,
            id = model,
            capabilities = OPENAI_COMPATIBLE_CAPABILITIES,
        )

        // TODO(@Enhancement): Anthropic capabilities differ (no OpenAIEndpoint).
        is AnthropicConfig -> error("AnthropicConfig → LLModel not implemented yet")

        else -> error("Unknown LlmProviderConfig: ${this::class.qualifiedName}")
    }

    private fun openAiCompatibleExecutor(apiKey: String, baseUrl: String): PromptExecutor {
        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(baseUrl = baseUrl),
            httpClientFactory = httpClientFactory,
        )
        return MultiLLMPromptExecutor(client)
    }

    private fun readApiKey(envVar: String): String {
        if (envVar.isBlank()) return ""              // local server — no key
        return System.getenv(envVar) ?: ""
    }

    companion object {

        private val OPENAI_COMPATIBLE_CAPABILITIES = listOf(
            LLMCapability.Completion,
            LLMCapability.OpenAIEndpoint.Completions,
        )
    }
}
