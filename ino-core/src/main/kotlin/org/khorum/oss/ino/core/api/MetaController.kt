package org.khorum.oss.ino.core.api

import org.khorum.oss.ino.core.api.dto.ProviderTypeDto
import org.khorum.oss.ino.core.api.dto.ToolSummaryDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only introspection endpoints surfacing the runtime's static
 * capabilities — provider types the bridge knows about, and tool
 * registration state.
 *
 * Tools aren't wired to a runtime registry yet (Koog's `ToolRegistry`
 * integration is the next deliverable), so `GET /api/tools` returns an empty
 * list for now. The endpoint exists so dashboard consumers can rely on the
 * shape; it'll start returning entries once tool wiring lands.
 */
@RestController
class MetaController {

    @GetMapping("/api/providers")
    fun listProviders(): List<ProviderTypeDto> = ProviderTypeDto.BUILT_INS

    @GetMapping("/api/tools")
    fun listTools(): List<ToolSummaryDto> = emptyList()
}
