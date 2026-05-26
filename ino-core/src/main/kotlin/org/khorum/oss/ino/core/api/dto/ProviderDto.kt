package org.khorum.oss.ino.core.api.dto

/**
 * Provider type metadata for the `GET /api/providers` introspection
 * endpoint. Lists the built-in provider variants the bridge can currently
 * resolve plus a flag for whether their config is wired end-to-end.
 *
 * Third-party `LlmProviderConfig` implementations (via `ProviderConfig.custom`)
 * aren't listed here — they're discoverable only through the agents that
 * use them.
 */
data class ProviderTypeDto(
    val id: String,
    val displayName: String,
    val supportsBaseUrlOverride: Boolean,
    val requiresApiKey: Boolean,
    val implemented: Boolean,
    val notes: String? = null,
) {
    companion object {
        val BUILT_INS: List<ProviderTypeDto> = listOf(
            ProviderTypeDto(
                id = "openai",
                displayName = "OpenAI (or compatible)",
                supportsBaseUrlOverride = true,
                requiresApiKey = true,
                implemented = true,
            ),
            ProviderTypeDto(
                id = "local",
                displayName = "Local OpenAI-compatible runtime",
                supportsBaseUrlOverride = true,
                requiresApiKey = false,
                implemented = true,
                notes = "llama.cpp / llama-server, vLLM, and Ollama's OpenAI shim all route through the OpenAI client.",
            ),
            ProviderTypeDto(
                id = "anthropic",
                displayName = "Anthropic",
                supportsBaseUrlOverride = true,
                requiresApiKey = true,
                implemented = false,
                notes = "Stubbed in the bridge — TODO(@Enhancement).",
            ),
        )
    }
}
