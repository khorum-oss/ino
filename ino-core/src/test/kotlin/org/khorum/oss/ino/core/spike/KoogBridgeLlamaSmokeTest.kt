package org.khorum.oss.ino.core.spike

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.khorum.oss.ino.core.koog.AgentBridge
import org.khorum.oss.ino.dsl.agent
import kotlin.test.assertTrue

/**
 * End-to-end test through the bridge: konstellation DSL → [AgentBridge] →
 * Koog `AIAgent` → llama-server → response.
 *
 * Requires llama-server running on `127.0.0.1:11435`:
 *
 *     llama-server -m <gguf> --host 127.0.0.1 --port 11435 --jinja
 *
 * Run with:
 *
 *     INO_LIVE_LLAMA=1 ./gradlew :ino-core:test \
 *       --tests "*KoogBridgeLlamaSmokeTest*"
 *
 * If this passes, the whole pipeline is wired: an agent declared in our DSL
 * can talk to a real local model with zero hand-written Koog plumbing.
 */
@EnabledIfEnvironmentVariable(named = "INO_LIVE_LLAMA", matches = "1")
class KoogBridgeLlamaSmokeTest {

    @Test
    fun `DSL-declared agent reaches llama-server through the bridge`() = runBlocking {
        val dslAgent = agent {
            name = "llama-bridge-smoke"
            description = "Smoke test for the konstellation -> Koog bridge"
            provider {
                local {
                    model = "qwen3-coder"
                    host = "http://127.0.0.1:11435"
                }
            }
            systemPrompt = "You are concise. Reply in one short sentence."
        }

        val koogAgent = AgentBridge().toKoogAgent(dslAgent)
        val result = koogAgent.run("Reply with the single word 'pong' and nothing else.")
        println("[bridge smoke] result = '$result'")
        assertTrue(result.isNotBlank(), "expected non-empty response, got: '$result'")
    }
}
