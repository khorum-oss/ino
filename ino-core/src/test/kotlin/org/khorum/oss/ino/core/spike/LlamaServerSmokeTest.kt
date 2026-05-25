package org.khorum.oss.ino.core.spike

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertTrue

/**
 * Live smoke test against a locally-running llama-server. Skipped unless
 * `INO_LIVE_LLAMA=1` is set, so it never runs in CI by accident.
 *
 * Run llama-server like this:
 *
 *     llama-server \
 *       -m ~/models/qwen3-coder-next/UD-Q6_K_XL/Qwen3-Coder-Next-UD-Q6_K_XL-00001-of-00003.gguf \
 *       -ngl 99 -c 262144 -fa on --jinja \
 *       --host 127.0.0.1 --port 11435
 *
 * Then execute:
 *
 *     INO_LIVE_LLAMA=1 ./gradlew :ino-core:test \
 *       --tests "*LlamaServerSmokeTest*" --info
 *
 * Proves the Koog → llama-server wire path works end-to-end. Once green,
 * the konstellation-DSL → Koog AIAgent bridge can be built on top.
 */
@EnabledIfEnvironmentVariable(named = "INO_LIVE_LLAMA", matches = "1")
class LlamaServerSmokeTest {

    @Test
    fun `koog AIAgent reaches llama-server and returns a non-empty response`() = runBlocking {
        // llama-server speaks OpenAI Chat Completions at /v1/chat/completions.
        // OpenAIClientSettings defaults to the right paths; we just override baseUrl.
        val client = OpenAILLMClient(
            apiKey = "",  // llama-server doesn't require an API key
            settings = OpenAIClientSettings(baseUrl = "http://127.0.0.1:11435"),
        )
        val executor = MultiLLMPromptExecutor(client)

        // llama-server accepts any model id string and routes to the loaded GGUF.
        // OpenAIEndpoint.Completions tells Koog to use the legacy Chat Completions
        // path (not the new Responses API, which llama-server doesn't speak).
        val model = LLModel(
            provider = LLMProvider.OpenAI,
            id = "qwen3-coder",
            capabilities = listOf(
                LLMCapability.OpenAIEndpoint.Completions,
                LLMCapability.Temperature,
                LLMCapability.Completion,
            ),
            contextLength = 262_144L,
        )

        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = "You are a concise assistant. Reply in one short sentence.",
        )

        val result = agent.run("Reply with the single word 'pong' and nothing else.")
        println("[llama-server smoke] result = '$result'")
        assertTrue(result.isNotBlank(), "expected non-empty response, got: '$result'")
    }
}
