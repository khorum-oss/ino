package org.khorum.oss.ino.core.agent

import ai.koog.agents.core.agent.AIAgent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.koog.AgentBridge
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.khorum.oss.ino.dsl.Agent
import org.khorum.oss.ino.dsl.agent
import java.time.Instant
import kotlin.test.assertEquals

/**
 * Unit-level tests for [AgentRunner]. The Koog `AIAgent` and `AgentBridge`
 * are mocked so these tests don't need a real LLM endpoint; the live wire
 * path is covered by `KoogBridgeLlamaSmokeTest`.
 */
class AgentRunnerTest {

    private val bridge: AgentBridge = mockk()
    private val store: ConversationStore = mockk(relaxed = true)
    private val registry: AgentRegistry = AgentRegistry(listOf(sampleAgent()))
    private val activeRuns: ActiveRunRegistry = ActiveRunRegistry()
    private val runner = AgentRunner(registry, store, bridge, activeRuns)

    private fun sampleAgent(name: String = "smoke"): Agent = agent {
        this.name = name
        provider {
            local {
                model = "test-model"
                host = "http://localhost:11434"
            }
        }
    }

    private fun sampleSession(
        id: String = "sess-1",
        agentName: String = "smoke",
        status: SessionStatus = SessionStatus.ACTIVE,
    ): Session = Session(
        id = id,
        agentName = agentName,
        providerId = "local",
        model = "test-model",
        status = status,
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun sampleMessage(content: String, role: MessageRole): Message = Message(
        id = "msg-${role.dbValue}",
        sessionId = "sess-1",
        seq = 0,
        role = role,
        content = content,
        createdAt = Instant.parse("2026-01-01T00:00:01Z"),
    )

    @Test
    fun `startSession persists a new session for the named agent`() {
        every { store.startSession(any(), any(), any(), any(), any()) } returns sampleSession()

        val dto = runner.startSession("smoke")

        assertEquals("sess-1", dto.id)
        assertEquals("smoke", dto.agentName)
        assertEquals("local", dto.providerId)
        assertEquals("test-model", dto.model)
        verify { store.startSession("smoke", "local", "test-model", any(), any()) }
    }

    @Test
    fun `startSession throws when the agent is not registered`() {
        assertThrows<NoSuchElementException> { runner.startSession("unknown") }
    }

    @Test
    fun `sendUserMessage persists user + assistant messages and returns the assistant DTO`() = runTest {
        val koogAgent: AIAgent<String, String> = mockk()
        every { store.findSession("sess-1") } returns sampleSession()
        every { bridge.toKoogAgent(any()) } returns koogAgent
        coEvery { koogAgent.run("hello") } returns "world"
        every { store.appendUser("sess-1", "hello") } returns sampleMessage("hello", MessageRole.USER)
        every { store.appendAssistant("sess-1", "world", any(), any(), any(), any()) } returns
            sampleMessage("world", MessageRole.ASSISTANT)

        val dto = runner.sendUserMessage("sess-1", "hello")

        assertEquals("world", dto.content)
        assertEquals(MessageRole.ASSISTANT.dbValue, dto.role)
        verify { store.appendUser("sess-1", "hello") }
        verify { store.appendAssistant("sess-1", "world", any(), any(), any(), any()) }
    }

    @Test
    fun `sendUserMessage throws when the session does not exist`() {
        every { store.findSession("missing") } returns null
        assertThrows<NoSuchElementException> {
            runBlocking { runner.sendUserMessage("missing", "hi") }
        }
    }

    @Test
    fun `sendUserMessage refuses to append to a non-active session`() {
        every { store.findSession("sess-1") } returns sampleSession(status = SessionStatus.COMPLETED)
        assertThrows<IllegalArgumentException> {
            runBlocking { runner.sendUserMessage("sess-1", "hi") }
        }
    }
}
