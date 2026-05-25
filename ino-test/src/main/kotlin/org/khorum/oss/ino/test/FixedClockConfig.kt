package org.khorum.oss.ino.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Deterministic clock for tests. Import via `@Import(FixedClockConfig::class)` on
 * any Spring test class to override the production `Clock.systemUTC()` bean.
 *
 * The fixed instant — 2026-01-01T00:00:00Z — is mirrored in
 * `docs/testing.md` so captured fixtures stay stable across the suite.
 */
@TestConfiguration
class FixedClockConfig {

    @Bean
    @Primary
    fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

    companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
