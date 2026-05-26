# Sample agents

How to declare new agents and register them with the runtime. The DSL itself
lives in `ino-dsl`; this doc shows real wiring against the three built-in
provider variants.

## Registering an agent

Agents are Spring `@Bean`s assembled in
[`SampleAgentsConfig`](../ino-core/src/main/kotlin/org/khorum/oss/ino/core/agent/SampleAgentsConfig.kt).
The `AgentRegistry` is a single bean that holds them all, keyed by name:

```kotlin
@Configuration
class SampleAgentsConfig {
    @Bean
    fun agentRegistry(): AgentRegistry = AgentRegistry(
        listOf(
            llamaLocalAgent(),
            researchAgent(),
            // ← add more here
        ),
    )

    private fun llamaLocalAgent(): Agent = agent {
        name = "llama-local"
        // … see below
    }
}
```

Names must be unique within the registry — the constructor fails fast on
duplicates.

Once registered, an agent shows up on the dashboard home page and at
`GET /api/agents`. To start a session pick "new session" in the UI or:

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"agent":"llama-local"}'
```

## Provider variants

### Local OpenAI-compatible runtime (llama-server, vLLM, Ollama-shim)

```kotlin
private fun llamaLocalAgent(): Agent = agent {
    name = "llama-local"
    description = "Local llama.cpp server (qwen3-coder by default)."
    provider {
        local {
            model = "qwen3-coder"
            host = "http://127.0.0.1:11435"
        }
    }
    systemPrompt = "You are a helpful, concise assistant. Reply in plain text."
}
```

`local { }` routes through Koog's `OpenAILLMClient` with `apiKey=""` and the
endpoint declared via `host`. Any server that exposes
`POST /v1/chat/completions` works — `llama.cpp`'s `llama-server`, vLLM, TGI,
or Ollama's `--openai`-compatible shim.

### OpenAI cloud

```kotlin
private fun researchAgent(): Agent = agent {
    name = "research"
    description = "Web-aware research assistant."
    provider {
        openai {
            model = "gpt-5"
            apiKeyEnvVar = "OPENAI_API_KEY"   // read at runtime
            temperature = 0.7
            maxOutputTokens = 4096
        }
    }
    systemPrompt = """
        You are a careful research assistant. Cite sources.
        Use the tools available before answering speculatively.
    """.trimIndent()
}
```

The API key is **never inlined** — `apiKeyEnvVar` is the *name* of the env
var the runtime reads. Set it in your shell before booting:

```bash
export OPENAI_API_KEY=sk-...
./gradlew :ino-core:bootRun
```

### Anthropic (stub)

```kotlin
private fun assistantAgent(): Agent = agent {
    name = "assistant"
    provider {
        anthropic {
            model = "claude-opus-4-7"
            apiKeyEnvVar = "ANTHROPIC_API_KEY"
        }
    }
}
```

The Anthropic branch in `AgentBridge` is currently stubbed — calls throw
`IllegalStateException: AnthropicConfig → Koog bridge not implemented yet`.
The DSL slot is reserved so declarations don't break when the bridge lands.
See the `@Enhancement` markers in `AgentBridge.kt`.

### Third-party / custom provider

`LlmProviderConfig` is an open interface, so anyone can implement a provider
config and inject it via the `custom` slot on `provider { }`:

```kotlin
private fun customAgent(): Agent = agent {
    name = "azure"
    provider {
        custom = AzureOpenAiConfig(model = "gpt-5", endpoint = "https://…")
    }
}
```

The bridge currently throws for unknown configs — wiring `custom` to a
runtime registry (`Map<KClass<*>, ProviderAdapter>`) is the next phase. The
DSL surface is stable today.

## Other configuration knobs

```kotlin
private fun researchAgent(): Agent = agent {
    name = "research"
    provider { /* … */ }
    systemPrompt = "…"

    // Bounded tool-call loops — protects against runaway agents.
    maxIterations = 50

    // Cost cap in USD micros. null = unlimited. The engine emits an
    // End event with stopReason=budget_exhausted once the cap is hit.
    budgetUsdMicros = 1_000_000   // 1 USD
}
```

## DSL surface (quick reference)

```
agent("name") {                         // root builder
    description = "…"
    provider {                          // exactly one slot must be set
        anthropic { model = "…" }       //   built-in
        openai    { model = "…" }       //   built-in
        local     { model = "…" }       //   built-in
        custom    = MyConfig(…)         //   third-party escape hatch
    }
    systemPrompt = "…"
    tools {                             // empty for chat-only agents
        tool {
            name = "web_search"
            description = "…"
            parameters {
                toolParameter {
                    name = "query"
                    typeSpec = ParameterTypeSpec.StringSpec
                    required()
                }
            }
        }
    }
    maxIterations = 25
    budgetUsdMicros = null
}
```

Field-by-field reference: [dsl.md](dsl.md).

## What's missing today

- **Tool dispatch.** The DSL accepts `tools { }` declarations and the
  registry returns them, but the bridge doesn't yet pass them to Koog's
  `ToolRegistry`. Phase 1.5 — see roadmap.
- **Fallback chains.** `AgentBridge` doesn't read fallback configs yet (the
  DSL doesn't expose them either; flagged via `@Enhancement` on `Agent`).
- **Conversation history threading.** Each streaming request sends just the
  system prompt + current user message — no prior turns. The sync path
  (`agent.run(input)`) does manage history internally via Koog.
