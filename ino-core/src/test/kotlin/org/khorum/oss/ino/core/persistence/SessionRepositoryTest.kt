package org.khorum.oss.ino.core.persistence

import org.junit.jupiter.api.Test
import org.khorum.oss.ino.core.domain.Session
import org.khorum.oss.ino.core.domain.SessionStatus
import org.khorum.oss.ino.core.util.UuidV7Generator
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionRepositoryTest : RepositorySliceTest() {

    private val ids by lazy { UuidV7Generator(clock) }
    private val repo by lazy { SessionRepository(jdbc, mapper, clock) }

    @Test
    fun `create then findById returns the same session`() {
        val session = Session(
            id = ids.nextString(),
            agentName = "writer",
            providerId = "anthropic",
            model = "claude-opus-4-7",
            status = SessionStatus.ACTIVE,
            metadata = mapOf("source" to "test", "tags" to listOf("a", "b")),
            startedAt = clock.instant(),
        )

        val saved = repo.create(session)
        val loaded = repo.findById(saved.id)

        assertNotNull(loaded)
        assertEquals(saved, loaded)
        assertEquals(mapOf("source" to "test", "tags" to listOf("a", "b")), loaded.metadata)
    }

    @Test
    fun `findById returns null for unknown id`() {
        assertNull(repo.findById(ids.nextString()))
    }

    @Test
    fun `listByAgent orders by started_at descending`() {
        val agent = "writer"
        val earlier = repo.create(sessionFixture(id = ids.nextString(), agent = agent, started = Instant.parse("2026-01-01T00:00:00Z")))
        val middle = repo.create(sessionFixture(id = ids.nextString(), agent = agent, started = Instant.parse("2026-01-02T00:00:00Z")))
        val latest = repo.create(sessionFixture(id = ids.nextString(), agent = agent, started = Instant.parse("2026-01-03T00:00:00Z")))
        repo.create(sessionFixture(id = ids.nextString(), agent = "other", started = Instant.parse("2026-01-04T00:00:00Z")))

        val result = repo.listByAgent(agent)

        assertEquals(listOf(latest.id, middle.id, earlier.id), result.map(Session::id))
    }

    @Test
    fun `updateStatus changes status and endedAt`() {
        val session = repo.create(sessionFixture(id = ids.nextString()))
        val endedAt = Instant.parse("2026-01-01T01:00:00Z")

        repo.updateStatus(session.id, SessionStatus.COMPLETED, endedAt)

        val loaded = assertNotNull(repo.findById(session.id))
        assertEquals(SessionStatus.COMPLETED, loaded.status)
        assertEquals(endedAt, loaded.endedAt)
    }

    @Test
    fun `bumpUsage accumulates tokens and cost`() {
        val session = repo.create(sessionFixture(id = ids.nextString()))

        repo.bumpUsage(session.id, inputTokens = 100, outputTokens = 50, costMicros = 2_500L)
        repo.bumpUsage(session.id, inputTokens = 200, outputTokens = 70, costMicros = 4_000L)

        val loaded = assertNotNull(repo.findById(session.id))
        assertEquals(300, loaded.totalInputTokens)
        assertEquals(120, loaded.totalOutputTokens)
        assertEquals(6_500L, loaded.totalCostUsdMicros)
    }

    private fun sessionFixture(
        id: String,
        agent: String = "writer",
        started: Instant = clock.instant(),
    ) = Session(
        id = id,
        agentName = agent,
        providerId = "anthropic",
        model = "claude-opus-4-7",
        status = SessionStatus.ACTIVE,
        startedAt = started,
    )
}
