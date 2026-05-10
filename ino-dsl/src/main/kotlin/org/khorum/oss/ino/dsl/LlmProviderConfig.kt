package org.khorum.oss.ino.dsl

import org.khorum.oss.ino.dsl.config.DEFAULT_ANTHROPIC_API_KEY_ENV_VAR
import org.khorum.oss.ino.dsl.config.DEFAULT_ANTHROPIC_VERSION
import org.khorum.oss.ino.dsl.config.DEFAULT_LOCAL_MODEL_HOST
import org.khorum.oss.ino.dsl.config.DEFAULT_OPENAI_API_KEY_ENV_VAR
import org.khorum.oss.ino.dsl.config.DEFAULT_TIMEOUT_SECONDS
import org.khorum.oss.konstellation.metaDsl.annotation.GeneratedDsl
import org.khorum.oss.konstellation.metaDsl.annotation.RootDsl
import org.khorum.oss.konstellation.metaDsl.annotation.TransientDsl
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.DefaultValue
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultFalse
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.NegationFunctionTemplate

/**
 * Marker contract for any LLM provider configuration. Built-in implementations
 * (`AnthropicConfig`, `OpenAiConfig`, `OllamaConfig`) live in this file; anyone
 * — including third-party module authors — can write their own implementation
 * to plug a new provider into ino.
 *
 * Pairing: a custom config implements this interface AND a matching
 * `LlmProvider` (in `ino-core`) registers itself via `META-INF/services`,
 * declaring `supports(config) = config is MyCustomConfig`. The engine asks the
 * registry, not a closed `when` — so the hierarchy stays open by design.
 */
interface LlmProviderConfig {
    val model: String
}

/**
 * The `provider { … }` block on an Agent. Exactly one inner config is set;
 * the `init` block enforces this. Three built-in slots (`anthropic`, `openai`,
 * `ollama`) are kept `private` so konstellation only emits their block helpers
 * — `provider { anthropic { … } }` works, but `provider { anthropic = … }`
 * does not. The fourth slot, `custom`, is public and accepts any
 * `LlmProviderConfig` implementation; this is the third-party escape hatch.
 *
 * Resulting DSL:
 *
 *     agent("research") {
 *         provider {
 *             anthropic { model = "claude-opus-4-7" }     // block style — built-ins
 *         }
 *     }
 *
 *     // or, for a third-party provider:
 *     agent("custom") {
 *         provider {
 *             custom = AzureOpenAiConfig(model = "gpt-5", endpoint = "...")
 *         }
 *     }
 *
 * The engine reads `agent.provider.selected` to get the chosen config and
 * dispatches it through `ProviderRegistry`.
 */
@RootDsl
@GeneratedDsl
data class ProviderConfig(
    private val anthropic: AnthropicConfig? = null,
    private val openai: OpenAiConfig? = null,
    private val local: LocalConfig? = null,
    val custom: LlmProviderConfig? = null,
) {
    init {
        val count = listOfNotNull(anthropic, openai, local, custom).size
        require(count == 1) {
            "Exactly one provider config must be set in `provider { … }`, got $count."
        }
    }

    /** The single chosen config, regardless of which slot held it. */
    @TransientDsl
    val selected: LlmProviderConfig
        get() = anthropic ?: openai ?: local ?: custom!!
}

/**
 * Anthropic (Messages API) configuration.
 *
 * Notes:
 *  - `apiKeyEnvVar` is the *name* of the env var to read at runtime; the
 *    actual key is never stored inline. Defaults to `ANTHROPIC_API_KEY`.
 *  - `anthropicVersion` is the API version header (`anthropic-version`); the
 *    SDK pins a default but exposing it lets you opt into new versions.
 */
@RootDsl
@GeneratedDsl
data class AnthropicConfig(
    override val model: String,
    @DefaultValue(DEFAULT_ANTHROPIC_API_KEY_ENV_VAR)
    val apiKeyEnvVar: String = DEFAULT_ANTHROPIC_API_KEY_ENV_VAR,
    val baseUrl: String? = null,
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    @DefaultValue(value = DEFAULT_TIMEOUT_SECONDS.toString())
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    @DefaultValue(value = DEFAULT_ANTHROPIC_VERSION)
    val anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION,
) : LlmProviderConfig

/**
 * OpenAI (Chat Completions / Responses) configuration.
 *
 * `organization` and `project` are headers some accounts require. Setting
 * `parallelToolCalls = false` is occasionally useful when a chain of tool
 * calls must be strictly sequential.
 */
@RootDsl
@GeneratedDsl
data class OpenAiConfig(
    override val model: String,
    @DefaultValue(DEFAULT_OPENAI_API_KEY_ENV_VAR)
    val apiKeyEnvVar: String = DEFAULT_OPENAI_API_KEY_ENV_VAR,
    val baseUrl: String? = null,
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    @DefaultValue(value = DEFAULT_TIMEOUT_SECONDS.toString())
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    val organization: String? = null,
    val project: String? = null,
    @DefaultFalse(negationTemplate = NegationFunctionTemplate.DISABLE)
    val parallelToolCalls: Boolean = false,
) : LlmProviderConfig

/**
 * Local configuration.
 *
 * No `apiKeyEnvVar` — Ollama runs locally over HTTP. `host` defaults to the
 * standard Ollama listening address. `keepAliveSeconds` controls how long the
 * model stays warm in memory after the last request; useful tuning knob for
 * local laptops.
 */
@RootDsl
@GeneratedDsl
data class LocalConfig(
    override val model: String,
    @DefaultValue(value = DEFAULT_LOCAL_MODEL_HOST)
    val host: String = DEFAULT_LOCAL_MODEL_HOST,
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    @DefaultValue(value = DEFAULT_TIMEOUT_SECONDS.toString())
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    val keepAliveSeconds: Int? = null,
) : LlmProviderConfig
