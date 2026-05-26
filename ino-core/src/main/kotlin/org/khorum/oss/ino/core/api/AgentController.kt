package org.khorum.oss.ino.core.api

import org.khorum.oss.ino.core.agent.AgentRegistry
import org.khorum.oss.ino.core.api.dto.AgentDetailsDto
import org.khorum.oss.ino.core.api.dto.AgentSummaryDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/agents")
class AgentController(
    private val registry: AgentRegistry,
) {

    @GetMapping
    fun list(): List<AgentSummaryDto> =
        registry.all().map(AgentSummaryDto::from)

    @GetMapping("/{name}")
    fun get(@PathVariable name: String): AgentDetailsDto {
        val agent = registry.findByName(name)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agent '$name' is not registered")
        return AgentDetailsDto.from(agent)
    }
}
