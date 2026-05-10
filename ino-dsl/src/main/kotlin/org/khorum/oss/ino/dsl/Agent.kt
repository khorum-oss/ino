package org.khorum.oss.ino.dsl

import org.khorum.oss.konstellation.metaDsl.annotation.GeneratedDsl
import org.khorum.oss.konstellation.metaDsl.annotation.ListDsl
import org.khorum.oss.konstellation.metaDsl.annotation.RootDsl
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.DefaultValue
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyList
import org.khorum.oss.konstellation.metaDsl.annotation.defaults.state.standard.DefaultEmptyString

/**
 * Root DSL type. An `Agent` is everything the engine needs to start a session:
 *  - identity (name)
 *  - which model + provider to use
 *  - what tools it can call
 *  - how it behaves (system prompt, iteration cap, budget)
 *
 * Generated DSL:
 *
 *     val researchAgent = agent("research") {
 *         description = "answers questions using web + filesystem"
 *         provider {
 *             anthropic { model = "claude-opus-4-7" }
 *         }
 *         systemPrompt = "You are a careful research assistant…"
 *         tools {
 *             tool {
 *                 name = "web_search"
 *                 description = "Search the web."
 *                 parameters {
 *                     toolParameter {
 *                         name = "query"
 *                         typeSpec = ParameterTypeSpec.StringSpec
 *                         required()
 *                     }
 *                 }
 *             }
 *         }
 *         maxIterations = 50          // optional override
 *         budgetUsdMicros = 1_000_000 // optional ($1 cap)
 *     }
 *
 * About `@ListDsl` on `tools` (decision #4 from earlier):
 *   When konstellation sees `@ListDsl` on a `List<X>` where `X` is itself a
 *   `@GeneratedDsl` data class, it generates a nested block. The outer block
 *   is named after the property (`tools { … }`), and inside, each element
 *   gets a helper derived from the contained class name (`tool { … }` from
 *   `Tool`). It's the same trick we used for `Tool.parameters: List<ToolParameter>`,
 *   which gave us `parameters { toolParameter { … } }`.
 */
@RootDsl
@GeneratedDsl
@Enhancement(
    description = "fallbacks: List<ProviderConfig> for graceful degradation when " +
        "the primary provider rate-limits or errors out; per-tool budget caps; " +
        "scheduled execution metadata for cron-driven runs"
)
data class Agent(
    val name: String,
    @DefaultEmptyString
    val description: String = "",

    /** The chosen LLM provider. See `LlmProviderConfig.kt` for the wrapper shape. */
    val provider: ProviderConfig,

    /**
     * Persona/memory/soul files are a separate
     * concern and will arrive in phase 2 as their own modules feeding the engine.
     */
    @DefaultEmptyString
    val systemPrompt: String = "",

    /** Tools available to this agent. Empty means a chat-only agent. */
    @ListDsl
    @DefaultEmptyList
    val tools: List<Tool> = emptyList(),

    /**
     * Maximum tool-call iterations before the engine forcibly stops the loop.
     * The default protects against runaway loops; override per-agent for
     * heavy-research scenarios.
     */
    @DefaultValue("25", "kotlin", "Int")
    val maxIterations: Int = 25,

    /**
     * Cost cap in USD micros (1 USD = 1_000_000 micros). `null` means
     * unlimited; non-null causes the engine to halt with `BUDGET_EXHAUSTED`
     * once cumulative cost crosses the cap.
     */
    val budgetUsdMicros: Long? = null,
)
