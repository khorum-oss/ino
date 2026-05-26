package org.khorum.oss.ino.core.persistence

import org.junit.jupiter.api.Test
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.domain.ToolInvocationStatus
import org.khorum.oss.ino.core.util.UuidV7Generator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConversationStoreTest : RepositorySliceTest() {

    private val store: ConversationStore by lazy {
        ConversationStore(
            sessions = SessionRepository(jdbc, mapper, clock),
            messages = MessageRepository(jdbc, mapper, clock),
            toolInvocations = ToolInvocationRepository(jdbc, clock),
            ids = UuidV7Generator(clock),
            clock = clock,
        )
    }

    @Test
    fun `full session lifecycle persists user assistant tool messages and invocation`() {
        val session = store.startSession(
            agentName = "writer",
            providerId = "anthropic",
            model = "claude-opus-4-7",
            metadata = mapOf("source" to "test"),
        )

        val userMessage = store.appendUser(session.id, "What's the weather?")

        val assistantMessage = store.appendAssistant(
            sessionId = session.id,
            content = "Let me check.",
            toolCalls = listOf(
                mapOf("id" to "call_1", "name" to "web_search", "arguments" to mapOf("q" to "weather")),
            ),
            inputTokens = 10,
            outputTokens = 5,
        )

        val invocation = store.recordToolInvocation(
            messageId = assistantMessage.id,
            toolName = "web_search",
            argumentsJson = """{"q":"weather"}""",
        )
        store.finishToolInvocation(
            invocationId = invocation.id,
            status = ToolInvocationStatus.SUCCESS,
            resultJson = """{"summary":"sunny"}""",
            durationMs = 250L,
        )

        store.appendToolResult(
            sessionId = session.id,
            toolCallId = "call_1",
            content = """{"summary":"sunny"}""",
        )

        store.bumpUsage(session.id, inputTokens = 10, outputTokens = 5, costMicros = 1_234L)
        store.finalize(session.id, SessionStatus.COMPLETED)

        val all = store.messagesFor(session.id)
        assertEquals(
            listOf(MessageRole.USER, MessageRole.ASSISTANT, MessageRole.TOOL),
            all.map { it.role },
        )
        assertEquals(listOf(0, 1, 2), all.map { it.seq })
        assertEquals(userMessage.id, all[0].id)
        assertEquals(assistantMessage.id, all[1].id)

        val savedInvocations = store.invocationsFor(assistantMessage.id)
        val saved = assertNotNull(savedInvocations.singleOrNull())
        assertEquals(ToolInvocationStatus.SUCCESS, saved.status)
        assertEquals("""{"summary":"sunny"}""", saved.resultJson)
        assertEquals(250L, saved.durationMs)

        val reloaded = assertNotNull(SessionRepository(jdbc, mapper, clock).findById(session.id))
        assertEquals(SessionStatus.COMPLETED, reloaded.status)
        assertEquals(10, reloaded.totalInputTokens)
        assertEquals(5, reloaded.totalOutputTokens)
        assertEquals(1_234L, reloaded.totalCostUsdMicros)
        assertNotNull(reloaded.endedAt)
    }

    @Test
    fun `messagesFor unknown session returns empty list`() {
        assertEquals(emptyList(), store.messagesFor("non-existent-id"))
    }

    @Test
    fun `invocation stays pending until finishToolInvocation is called`() {
        val session = store.startSession("writer", "anthropic", "claude-opus-4-7")
        val assistant = store.appendAssistant(session.id, "calling tool")
        val invocation = store.recordToolInvocation(assistant.id, "web_search", "{}")

        val saved = assertNotNull(store.invocationsFor(assistant.id).singleOrNull())
        assertEquals(ToolInvocationStatus.PENDING, saved.status)
        assertNull(saved.finishedAt)
        assertNull(saved.durationMs)
        assertEquals(invocation.id, saved.id)
    }
}
