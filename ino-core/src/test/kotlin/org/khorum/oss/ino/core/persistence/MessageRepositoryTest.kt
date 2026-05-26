package org.khorum.oss.ino.core.persistence

import org.junit.jupiter.api.Test
import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.util.UuidV7Generator
import java.time.Instant
import kotlin.test.assertEquals

class MessageRepositoryTest : RepositorySliceTest() {

    private val ids by lazy { UuidV7Generator(clock) }
    private val sessions by lazy { SessionRepository(jdbc, mapper, clock) }
    private val repo by lazy { MessageRepository(jdbc, mapper, clock) }

    private fun seedSession(): Session = sessions.create(
        Session(
            id = ids.nextString(),
            agentName = "writer",
            providerId = "anthropic",
            model = "claude-opus-4-7",
            status = SessionStatus.ACTIVE,
            startedAt = clock.instant(),
        ),
    )

    @Test
    fun `append assigns monotonic seq starting at zero`() {
        val session = seedSession()

        val first = repo.append(messageFixture(sessionId = session.id, content = "hello"))
        val second = repo.append(messageFixture(sessionId = session.id, content = "world"))
        val third = repo.append(messageFixture(sessionId = session.id, role = MessageRole.ASSISTANT, content = "hi"))

        assertEquals(0, first.seq)
        assertEquals(1, second.seq)
        assertEquals(2, third.seq)
        assertEquals(3, repo.countBySession(session.id))
    }

    @Test
    fun `listBySession returns messages in seq order`() {
        val session = seedSession()
        val a = repo.append(messageFixture(sessionId = session.id, content = "one"))
        val b = repo.append(messageFixture(sessionId = session.id, content = "two"))
        val c = repo.append(messageFixture(sessionId = session.id, content = "three"))

        val all = repo.listBySession(session.id)
        assertEquals(listOf(a.id, b.id, c.id), all.map(Message::id))
        assertEquals(listOf(0, 1, 2), all.map(Message::seq))
    }

    @Test
    fun `tool_calls_json round-trips through Jackson`() {
        val session = seedSession()
        val toolCalls = listOf(
            mapOf("id" to "call_1", "name" to "web_search", "arguments" to mapOf("query" to "weather")),
        )
        val saved = repo.append(
            messageFixture(sessionId = session.id, role = MessageRole.ASSISTANT, toolCalls = toolCalls),
        )

        val loaded = repo.listBySession(session.id).single()
        assertEquals(saved.id, loaded.id)
        assertEquals(toolCalls, loaded.toolCalls)
    }

    @Test
    fun `seq counter is independent per session`() {
        val sessionA = seedSession()
        val sessionB = seedSession()

        repo.append(messageFixture(sessionId = sessionA.id, content = "a1"))
        repo.append(messageFixture(sessionId = sessionA.id, content = "a2"))
        val bFirst = repo.append(messageFixture(sessionId = sessionB.id, content = "b1"))

        assertEquals(0, bFirst.seq)
        assertEquals(2, repo.countBySession(sessionA.id))
        assertEquals(1, repo.countBySession(sessionB.id))
    }

    private fun messageFixture(
        sessionId: String,
        role: MessageRole = MessageRole.USER,
        content: String? = null,
        toolCalls: List<Map<String, Any?>>? = null,
        createdAt: Instant = clock.instant(),
    ) = Message(
        id = ids.nextString(),
        sessionId = sessionId,
        seq = -1,
        role = role,
        content = content,
        toolCalls = toolCalls,
        createdAt = createdAt,
    )
}
