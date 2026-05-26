package org.khorum.oss.ino.core.agent

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActiveRunRegistryTest {

    @Test
    fun `cancel returns false when no run is registered`() {
        val reg = ActiveRunRegistry()
        assertFalse(reg.cancel("nothing"))
    }

    @Test
    fun `cancel returns true and cancels the registered job`() = runTest {
        val reg = ActiveRunRegistry()

        var ran = false
        val job: Job = launch {
            try {
                delay(100_000)  // would hang the test if not cancelled
                ran = true
            } catch (_: kotlinx.coroutines.CancellationException) {
                // expected
            }
        }
        reg.register("sess", job)
        assertTrue(reg.isActive("sess"))

        val cancelled = reg.cancel("sess")

        assertTrue(cancelled)
        assertFalse(reg.isActive("sess"), "registry should remove the entry on cancel")
        job.join()
        assertFalse(ran, "delay should have been cancelled before completing")
    }

    @Test
    fun `deregister removes the entry without cancelling the job`() = runTest {
        val reg = ActiveRunRegistry()
        val job = launch { delay(1) }
        reg.register("sess", job)

        reg.deregister("sess")

        assertFalse(reg.isActive("sess"))
        assertFalse(job.isCancelled, "deregister must not cancel")
        job.join()
    }

    @Test
    fun `register replaces an existing entry`() = runTest {
        val reg = ActiveRunRegistry()
        val first = launch { delay(100_000) }
        val second = launch { delay(100_000) }
        reg.register("sess", first)
        reg.register("sess", second)

        // cancel hits `second`, not `first`
        reg.cancel("sess")

        assertTrue(second.isCancelled)
        assertFalse(first.isCancelled, "the displaced job should not be cancelled by the registry")
        first.cancel()  // cleanup
    }
}
