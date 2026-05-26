package org.khorum.oss.ino.core.persistence

import org.junit.jupiter.api.Test
import org.khorum.oss.ino.core.domain.Message
import org.khorum.oss.ino.core.domain.MessageRole
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.domain.ToolInvocation
import org.khorum.oss.ino.core.domain.ToolInvocationStatus
import org.khorum.oss.ino.core.util.UuidV7Generator
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ToolInvocationRepositoryTest : RepositorySliceTest() {

    private val ids by lazy { UuidV7Generator(clock) }
    private val sessions by lazy { SessionRepository(jdbc, mapper, clock) }
    private val messages by lazy { MessageRepository(jdbc, mapper, clock) }
    private val repo by lazy { ToolInvocationRepository(jdbc, clock) }

    private fun seedMessage(): Message {
        val session = sessions.create(
            Session(
                id = ids.nextString(),
                agentName = "writer",
                providerId = "anthropic",
                model = "claude-opus-4-7",
                status = SessionStatus.ACTIVE,
                startedAt = clock.instant(),
            ),
        )
        return messages.append(
            Message(
                id = ids.nextString(),
                sessionId = session.id,
                seq = -1,
                role = MessageRole.ASSISTANT,
                content = "calling tool",
                createdAt = clock.instant(),
            ),
        )
    }

    @Test
    fun `create then findById returns pending invocation`() {
        val message = seedMessage()
        val invocation = invocationFixture(messageId = message.id, status = ToolInvocationStatus.PENDING)

        val saved = repo.create(invocation)
        val loaded = assertNotNull(repo.findById(saved.id))

        assertEquals(saved, loaded)
        assertEquals(ToolInvocationStatus.PENDING, loaded.status)
    }

    @Test
    fun `complete transitions pending invocation to success`() {
        val message = seedMessage()
        val saved = repo.create(invocationFixture(messageId = message.id, status = ToolInvocationStatus.PENDING))
        val finishedAt = Instant.parse("2026-01-01T00:00:05Z")

        repo.complete(
            id = saved.id,
            status = ToolInvocationStatus.SUCCESS,
            resultJson = """{"output":"42"}""",
            finishedAt = finishedAt,
            durationMs = 5_000L,
        )

        val loaded = assertNotNull(repo.findById(saved.id))
        assertEquals(ToolInvocationStatus.SUCCESS, loaded.status)
        assertEquals("""{"output":"42"}""", loaded.resultJson)
        assertEquals(finishedAt, loaded.finishedAt)
        assertEquals(5_000L, loaded.durationMs)
    }

    @Test
    fun `complete records error payload`() {
        val message = seedMessage()
        val saved = repo.create(invocationFixture(messageId = message.id, status = ToolInvocationStatus.PENDING))

        repo.complete(
            id = saved.id,
            status = ToolInvocationStatus.ERROR,
            errorJson = """{"type":"TimeoutError","message":"deadline exceeded"}""",
            durationMs = 30_000L,
        )

        val loaded = assertNotNull(repo.findById(saved.id))
        assertEquals(ToolInvocationStatus.ERROR, loaded.status)
        assertEquals("""{"type":"TimeoutError","message":"deadline exceeded"}""", loaded.errorJson)
    }

    @Test
    fun `listByMessage returns invocations ordered by startedAt`() {
        val message = seedMessage()
        val first = repo.create(invocationFixture(messageId = message.id, startedAt = Instant.parse("2026-01-01T00:00:01Z")))
        val second = repo.create(invocationFixture(messageId = message.id, startedAt = Instant.parse("2026-01-01T00:00:02Z")))
        val third = repo.create(invocationFixture(messageId = message.id, startedAt = Instant.parse("2026-01-01T00:00:03Z")))

        val ordered = repo.listByMessage(message.id)
        assertEquals(listOf(first.id, second.id, third.id), ordered.map(ToolInvocation::id))
    }

    private fun invocationFixture(
        messageId: String,
        status: ToolInvocationStatus = ToolInvocationStatus.PENDING,
        startedAt: Instant = clock.instant(),
    ) = ToolInvocation(
        id = ids.nextString(),
        messageId = messageId,
        toolName = "web_search",
        argumentsJson = """{"query":"weather"}""",
        status = status,
        startedAt = startedAt,
    )
}
