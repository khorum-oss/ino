package org.khorum.oss.ino.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Serves the bundled SvelteKit dashboard at /dashboard/ and below.
 *
 * Spring WebFlux's static-resource handler serves files from
 * `classpath:/static/dashboard/` automatically, but it has two gaps:
 *
 *  1. No directory-index resolution — GET /dashboard/ returns 404 unless
 *     we map it explicitly to index.html.
 *  2. No SPA fallback — deep links like /dashboard/sessions/abc-123 don't
 *     match a real file and would 404. SvelteKit's adapter-static is
 *     configured with `fallback: 'index.html'` so the server returns
 *     index.html for any unmatched dashboard route and client-side
 *     routing takes over.
 *
 * The router below covers both. Real static assets (the _app/ JS chunks,
 * favicon.svg, etc.) are served before this router by Spring's
 * static-resource handler.
 */
@Configuration
class DashboardRouterConfig {

    private val index = ClassPathResource("static/dashboard/index.html")

    @Bean
    fun dashboardRouter(): RouterFunction<ServerResponse> = router {
        // /dashboard (no slash) — redirect to /dashboard/ so the base URL is canonical
        GET("/dashboard") {
            ServerResponse.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create("/dashboard/"))
                .build()
        }
        // Directory root and unmatched deep links both serve index.html.
        // Deep-link patterns are narrow on purpose so we don't accidentally
        // mask 404s for genuinely-missing assets under /dashboard/_app/.
        GET("/dashboard/") { serveIndex() }
        GET("/dashboard/sessions/{*rest}") { serveIndex() }
        GET("/dashboard/agents/{*rest}") { serveIndex() }
    }

    private fun serveIndex(): Mono<ServerResponse> =
        ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValue(index)
}
