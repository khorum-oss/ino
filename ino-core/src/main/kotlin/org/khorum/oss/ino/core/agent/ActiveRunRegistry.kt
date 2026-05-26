package org.khorum.oss.ino.core.agent

import kotlinx.coroutines.Job
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tracks in-flight streaming runs by session id so an external `DELETE
 * /api/sessions/{id}/run` can cancel them.
 *
 * The held value is the [Job] of the coroutine collecting the agent's stream.
 * Cancelling that job propagates through the kotlinx Flow, terminates the
 * upstream Koog `executeStreaming` collection with `CancellationException`,
 * runs the `onCompletion` block in the runner (which persists whatever text
 * was accumulated), and closes the SSE channel from the controller side.
 *
 * If a session has no active run, [cancel] returns `false` so the controller
 * can return `404 Not Found`.
 */
@Component
class ActiveRunRegistry {

    private val jobs = ConcurrentHashMap<String, Job>()

    fun register(sessionId: String, job: Job) {
        jobs[sessionId] = job
    }

    fun deregister(sessionId: String) {
        jobs.remove(sessionId)
    }

    /** Returns true if a run was present and was cancelled. */
    fun cancel(sessionId: String): Boolean {
        val job = jobs.remove(sessionId) ?: return false
        job.cancel(CancellationException("Run cancelled via API: $sessionId"))
        return true
    }

    fun isActive(sessionId: String): Boolean = jobs.containsKey(sessionId)
}
