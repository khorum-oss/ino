package org.khorum.oss.ino.test

/**
 * Best-effort scrub of provider API keys for tests.
 *
 * The JVM can't unset real OS env vars from Java/Kotlin, so this only clears
 * matching system properties — but combined with provider adapters that prefer
 * `System.getProperty()` over `System.getenv()` at lookup time in test mode,
 * it stops a stray live key from leaking into a fixture replay run.
 *
 * Call once from a `@BeforeAll` in your provider-adapter test base class.
 */
object CredentialScrubber {

    val SCRUBBED_KEYS: List<String> = listOf(
        "ANTHROPIC_API_KEY",
        "OPENAI_API_KEY",
        "OLLAMA_API_KEY",
    )

    fun scrub() {
        SCRUBBED_KEYS.forEach(System::clearProperty)
    }
}
