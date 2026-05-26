package org.khorum.oss.ino.core.api

import kotlinx.coroutines.reactor.asFlux
import org.khorum.oss.ino.core.agent.AgentRunner
import org.khorum.oss.ino.core.api.dto.AppendMessageRequest
import org.khorum.oss.ino.core.api.dto.MessageDto
import org.khorum.oss.ino.core.api.dto.StreamEventDto
import org.khorum.oss.ino.core.persistence.ConversationStore
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
class MessageController(
    private val runner: AgentRunner,
    private val store: ConversationStore,
) {

    /**
     * Append a user message and synchronously run the agent. Returns the
     * final assistant message DTO.
     *
     * For incremental delivery, use the SSE variant of this endpoint
     * (`Accept: text/event-stream`).
     */
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun send(
        @PathVariable sessionId: String,
        @RequestBody request: AppendMessageRequest,
    ): MessageDto {
        if (request.content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank")
        }
        return runner.sendUserMessage(sessionId, request.content)
    }

    /**
     * Stream the agent's response as SSE. Each event is a [StreamEventDto];
     * the SSE `event:` field carries the type discriminator (`text-delta`,
     * `tool-call-args-delta`, `end`, etc.).
     *
     * Routed via `Accept: text/event-stream` content negotiation — same path
     * as the sync endpoint.
     */
    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(
        @PathVariable sessionId: String,
        @RequestBody request: AppendMessageRequest,
    ): Flux<ServerSentEvent<StreamEventDto>> {
        if (request.content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank")
        }
        return runner.streamUserMessage(sessionId, request.content)
            .asFlux()
            .map { event ->
                ServerSentEvent.builder(event)
                    .event(event.type)
                    .build()
            }
    }

    @GetMapping
    fun history(@PathVariable sessionId: String): List<MessageDto> {
        if (store.findSession(sessionId) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session $sessionId not found")
        }
        return store.messagesFor(sessionId).map(MessageDto::from)
    }
}
