package org.khorum.oss.ino.core.util

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

/**
 * UUID v7 generator per RFC 9562 §5.7.
 *
 * Layout (big-endian):
 *   48 bits  unix_ts_ms
 *    4 bits  ver = 0b0111
 *   12 bits  rand_a
 *    2 bits  var = 0b10
 *   62 bits  rand_b
 *
 * Lex-sort on the resulting string equals chrono-sort within the same millisecond
 * batch — exactly the property we want for SQLite TEXT primary keys.
 */
class UuidV7(
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom(),
) {

    fun next(): UUID {
        val tsMs = clock.millis()
        val randA = random.nextInt(0x1000)
        val randB = random.nextLong()

        val msb = ((tsMs and TS_MASK) shl 16) or
            (VERSION_BITS shl 12) or
            randA.toLong()
        val lsb = (randB and LSB_RAND_MASK) or VARIANT_BITS

        return UUID(msb, lsb)
    }

    fun nextString(): String = next().toString()

    companion object {
        private const val TS_MASK = 0xFFFF_FFFF_FFFFL
        private const val VERSION_BITS = 0x7L
        // Variant 0b10 in the top two bits of the LSB == Long.MIN_VALUE.
        private const val VARIANT_BITS = Long.MIN_VALUE
        private const val LSB_RAND_MASK = 0x3FFF_FFFF_FFFF_FFFFL
    }
}
