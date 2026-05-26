# ino

Kotlin/Svelte agentic framework. Declare an agent with a typed DSL, chat with
it through a streaming SSE API, watch it work in a cyberpunk-themed Svelte
dashboard.

`ino-dsl` is a konstellation-generated DSL for agents, tools, and provider
configs. `ino-core` is a Spring Boot host that translates DSL declarations
into [Koog](https://github.com/jetbrains/koog) `AIAgent`s, persists session
state in SQLite, and serves the dashboard. `ino-dashboard` is the SvelteKit
UI.

## Module layout

```
ino/
├── ino-dsl/          # @GeneratedDsl data classes (Agent, Tool, ProviderConfig, …)
├── ino-core/         # Spring Boot 4.1 — AgentBridge, persistence, REST + SSE
├── ino-test/         # JUnit fixtures (InoHomeExtension, scrubbers, fixed Clock)
├── ino-dashboard/    # SvelteKit + Tailwind v4 + Vitest + Playwright
└── docs/             # architecture, dsl, engine, persistence, api, etc.
```

See [docs/](docs/) for the deep dive — start with [docs/README.md](docs/README.md).

## Quickstart

### 1. Start an LLM endpoint

`ino` ships configured for a **local** OpenAI-compatible runtime. The simplest
option is [`llama.cpp`'s built-in server](https://github.com/ggerganov/llama.cpp/tree/master/examples/server):

```bash
llama-server \
  -m ~/models/<your-model>.gguf \
  -ngl 99 -c 32768 -fa on --jinja \
  --host 127.0.0.1 --port 11435
```

vLLM, Ollama's OpenAI shim, and TGI all work the same way — anything that
speaks `POST /v1/chat/completions`. Cloud providers (OpenAI, Anthropic via
configured api keys) also work; see [Adding an agent](#adding-an-agent).

### 2. Build the dashboard (one time)

```bash
cd ino-dashboard
npm install
```

This is optional but recommended — once `node_modules/` is present, the
Gradle build automatically rebuilds the dashboard and serves it at
`http://localhost:8080/dashboard/`. Without it, `ino-core` runs API-only.

### 3. Run the backend

```bash
cd ino-core
./gradlew bootRun
```

This boots Spring Boot on `:8080`, applies Liquibase migrations against
`~/.ino/state.db`, wires Koog beans, and (if the dashboard is built) serves
the SvelteKit app at `/dashboard/`.

### 4. Open the dashboard

- Embedded (production-like): http://localhost:8080/dashboard/
- Dev mode with HMR: `cd ino-dashboard && npm run dev` → http://localhost:5173

Click "new session" on the `llama-local` agent card. Type a message. Watch
the response stream in token-by-token with a blinking cursor.

## Adding an agent

Agents are declared as Spring `@Bean`s using the konstellation DSL. To add
one, edit
[`ino-core/.../agent/SampleAgentsConfig.kt`](ino-core/src/main/kotlin/org/khorum/oss/ino/core/agent/SampleAgentsConfig.kt):

```kotlin
@Bean
fun agentRegistry(): AgentRegistry = AgentRegistry(
    listOf(
        llamaLocalAgent(),
        researchAgent(),   // ← your new one
    ),
)

private fun researchAgent(): Agent = agent {
    name = "research"
    description = "Web-aware research assistant."
    provider {
        openai {
            model = "gpt-5"
            apiKeyEnvVar = "OPENAI_API_KEY"
        }
    }
    systemPrompt = "You are a careful research assistant. Cite sources."
}
```

Three provider blocks today:

```kotlin
provider {
    anthropic { model = "claude-opus-4-7"; apiKeyEnvVar = "ANTHROPIC_API_KEY" }
}
provider {
    openai { model = "gpt-5"; baseUrl = "https://api.openai.com" }
}
provider {
    local { model = "qwen3-coder"; host = "http://127.0.0.1:11435" }
}
```

The `local { … }` block covers llama-server, vLLM, Ollama's OpenAI shim, or
any local OpenAI-compatible runtime. See [docs/dsl.md](docs/dsl.md) for the
full DSL surface.

## Smoke-testing against a real model

Two live tests are gated by `INO_LIVE_LLAMA=1` so they never run in CI by
accident:

```bash
# llama-server must be on 127.0.0.1:11435 first
cd ino-core
INO_LIVE_LLAMA=1 ./gradlew test --tests "*LlamaServerSmokeTest*"
INO_LIVE_LLAMA=1 ./gradlew test --tests "*KoogBridgeLlamaSmokeTest*"
INO_LIVE_LLAMA=1 ./gradlew test --tests "*StreamingSmokeTest*"
```

The first hand-wires Koog → llama-server. The second goes through the DSL →
`AgentBridge` → Koog path. The third also exercises persistence + the
streaming `Flow<StreamEventDto>`.

## API

All endpoints under `/api/`. JSON request/response except where noted.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/sessions` | body: `{"agent":"name"}` |
| `GET` | `/api/sessions/{id}` | session metadata |
| `GET` | `/api/sessions?agent=…` | list (agent filter required for v1) |
| `GET` | `/api/sessions/{id}/messages` | full history |
| `POST` | `/api/sessions/{id}/messages` | body: `{"content":"…"}`; returns final assistant message JSON |
| `POST` | `/api/sessions/{id}/messages` + `Accept: text/event-stream` | streams `StreamEventDto`s as SSE |
| `DELETE` | `/api/sessions/{id}/run` | cancel in-flight stream; preserves partial state |
| `GET` | `/api/agents` | registered agent summaries |
| `GET` | `/api/agents/{name}` | full agent definition incl. tools |
| `GET` | `/api/providers` | provider type metadata |
| `GET` | `/api/tools` | registered tools (empty until tool wiring lands) |

See [docs/api.md](docs/api.md) for DTO shapes and the SSE event wire format.

## Tests

```bash
# backend
cd ino-core && ./gradlew test
# 45 tests; 3 skipped (live smokes need llama-server)

# dashboard
cd ino-dashboard
npm run check       # svelte-check, types
npm run test:unit   # vitest
npm run test:e2e    # playwright (requires browser install: npx playwright install)
```

## Conventions

| Concern | Choice |
|---|---|
| Group | `org.khorum.oss.ino` |
| JVM | Java 21 |
| Kotlin | 2.3.10 (`ino-core`/`ino-test`), 2.1.20 (`ino-dsl`, pinned to konstellation KSP) |
| Spring Boot | 4.1.0-M1 |
| Agent runtime | Koog 1.0.0 (`ai.koog:koog-agents:1.0.0`) |
| Persistence | SQLite (`~/.ino/state.db`) + Liquibase |
| Timestamps | ISO-8601 UTC TEXT |
| Costs | `Long` micros (no floats) |
| IDs | UUID v7 |

## License

MIT
