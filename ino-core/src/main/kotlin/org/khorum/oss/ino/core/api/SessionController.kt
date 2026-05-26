package org.khorum.oss.ino.core.api

import org.khorum.oss.ino.core.agent.ActiveRunRegistry
import org.khorum.oss.ino.core.agent.AgentRegistry
import org.khorum.oss.ino.core.agent.AgentRunner
import org.khorum.oss.ino.core.api.dto.CreateSessionRequest
import org.khorum.oss.ino.core.api.dto.SessionDto
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val runner: AgentRunner,
    private val store: ConversationStore,
    private val registry: AgentRegistry,
    private val activeRuns: ActiveRunRegistry,
) {

    // ai: comments
    @PostMapping
    fun create(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionDto> {
        if (registry.findByName(request.agent) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agent '${request.agent}' is not registered")
        }
        val dto = runner.startSession(request.agent)
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): SessionDto {
        val session = store.findSession(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session $id not found")
        return SessionDto.from(session)
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) agent: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): List<SessionDto> {
        val resolvedLimit = limit.coerceIn(1, MAX_PAGE_SIZE)
        val resolvedOffset = offset.coerceAtLeast(0)
        // Phase 1: requires an agent filter. Cross-agent listing arrives with
        // pagination + search in phase 2.
        val agentName = agent
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agent query param is required for v1")
        return store.listSessionsByAgent(agentName, resolvedLimit, resolvedOffset).map(SessionDto::from)
    }

    /**
     * Cancel an in-flight streaming run. Returns 204 if a run was active and
     * cancelled, 404 if there was no active run for the session.
     *
     * Persisted state survives — whatever text accumulated before the cancel
     * is saved as an `assistant` message (see `AgentRunner.onCompletion`).
     */
    @DeleteMapping("/{id}/run")
    fun cancelRun(@PathVariable id: String): ResponseEntity<Void> {
        if (store.findSession(id) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session $id not found")
        }
        return if (activeRuns.cancel(id)) {
            ResponseEntity.noContent().build()
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No active run for session $id")
        }
    }

    companion object {
        const val MAX_PAGE_SIZE: Int = 200
    }
}
