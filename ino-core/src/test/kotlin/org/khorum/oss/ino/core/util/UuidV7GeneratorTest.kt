package org.khorum.oss.ino.core.util

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidV7GeneratorTest {

    @Test
    fun `version nibble is 7`() {
        val uuid = UuidV7Generator().next()
        assertEquals(7, uuid.version())
    }

    @Test
    fun `variant is RFC 4122`() {
        val uuid = UuidV7Generator().next()
        assertEquals(2, uuid.variant())
    }

    @Test
    fun `embedded timestamp matches clock`() {
        val instant = Instant.parse("2026-05-23T12:00:00Z")
        val fixed = Clock.fixed(instant, ZoneOffset.UTC)
        val uuid = UuidV7Generator(fixed).next()
        val embeddedMs = uuid.mostSignificantBits ushr 16
        assertEquals(instant.toEpochMilli(), embeddedMs)
    }

    @Test
    fun `lexicographic sort matches chronological sort across millisecond boundary`() {
        val earlier = UuidV7Generator(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)).nextString()
        val later = UuidV7Generator(Clock.fixed(Instant.parse("2026-01-01T00:00:01Z"), ZoneOffset.UTC)).nextString()
        assertTrue(earlier < later, "$earlier should sort before $later")
    }
}
