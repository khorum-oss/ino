package org.khorum.oss.ino.core.spike

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.khorum.oss.ino.core.agent.ActiveRunRegistry
import org.khorum.oss.ino.core.agent.AgentRegistry
import org.khorum.oss.ino.core.agent.AgentRunner
import org.khorum.oss.ino.core.api.dto.StreamEventDto
import org.khorum.oss.ino.core.config.koog.AgentBridge
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.khorum.oss.ino.dsl.agentDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteRecursively
import kotlin.test.assertTrue

/**
 * Live end-to-end test of the streaming path:
 *   AgentRunner.streamUserMessage → Koog.executeStreaming → llama-server → SSE-shaped DTOs
 *
 * The runner is invoked with a Spring-managed context so the persistence
 * side-effect (assistant message append on completion) is real, not mocked.
 *
 * Requires llama-server on `127.0.0.1:11435` and `INO_LIVE_LLAMA=1`:
 *
 *     llama-server -m <gguf> --host 127.0.0.1 --port 11435 --jinja
 *     INO_LIVE_LLAMA=1 ./gradlew :ino-core:test --tests "*StreamingSmokeTest*"
 */
@EnabledIfEnvironmentVariable(named = "INO_LIVE_LLAMA", matches = "1")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StreamingSmokeTest {

    @Autowired private lateinit var store: ConversationStore
    @Autowired private lateinit var bridge: AgentBridge

    @Test
    fun `streaming run emits text deltas and persists the assistant message`() = runBlocking {
        // Build a one-off agent via DSL — exercises konstellation → bridge → runner.
        val dslAgent = agentDefinition {
            name = "streaming-smoke"
            description = "ad-hoc agent used by StreamingSmokeTest"
            provider {
                local {
                    model = "qwen3.6-35b-a3b"
                    host = "http://127.0.0.1:11436"
                }
            }
            systemPrompt = "You are concise. Reply with exactly one short sentence."
        }
        val registry = AgentRegistry(listOf(dslAgent))
        val runner = AgentRunner(registry, store, bridge, ActiveRunRegistry())

        // Open a session directly via the store (skipping the controller layer).
        val sessionDto = runner.startSession(dslAgent.name)

        val events: List<StreamEventDto> = runner
            .streamUserMessage(sessionDto.id, "Say 'pong' and nothing else.")
            .toList()

        println("[streaming smoke] event count = ${events.size}")
        events.take(20).forEach { println("[streaming smoke] $it") }

        assertTrue(events.isNotEmpty(), "expected at least one stream event")
        assertTrue(
            events.any { it.type == StreamEventDto.TYPE_TEXT_DELTA },
            "expected at least one text-delta event in: ${events.map(StreamEventDto::type)}",
        )
        assertTrue(
            events.any { it.type == StreamEventDto.TYPE_END },
            "expected an end event in: ${events.map(StreamEventDto::type)}",
        )

        // Persistence side-effect: assistant message should be in history.
        val history = store.messagesFor(sessionDto.id)
        val assistant = history.singleOrNull { it.role.dbValue == "assistant" }
        assertTrue(assistant != null, "expected exactly one assistant message; got history: $history")
        assertTrue(!assistant!!.content.isNullOrBlank(), "assistant message should have content")
    }

    companion object {
        private lateinit var tempHome: Path

        @JvmStatic
        @org.junit.jupiter.api.BeforeAll
        fun allocateInoHome() {
            tempHome = Files.createTempDirectory("ino-streamsmoke-")
            System.setProperty("ino.home", tempHome.toString())
        }

        @JvmStatic
        @org.junit.jupiter.api.AfterAll
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        fun cleanupInoHome() {
            System.clearProperty("ino.home")
            tempHome.deleteRecursively()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                "jdbc:sqlite:${System.getProperty("ino.home")}/state.db"
            }
        }
    }
}
