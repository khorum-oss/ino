# Architecture

`ino` follows the **spektr-mirror** shape: a tiny Spring Boot core that loads provider/tool implementations as runtime extension JARs via `ServiceLoader`. The framework itself stays small; everything extensible ships as an independently versioned artifact.

## Why this shape

Three forces drove the choice:

1. **Framework first, product later.** The MVP must produce a usable, publishable framework. Phase-2 features (skills, memory, gateways, MCP, cron) become new extension JARs without changing the core.
2. **Reuse what works.** `spektr` already runs this pattern in production for REST/SOAP endpoints. The Gradle plugin, publishing pipeline, CI templates, and verification-metadata flow are already wired up for this exact module shape.
3. **Open extension, closed core.** Anyone can write a new `LlmProvider` or `ToolHandler` without touching `ino-core` source — they ship a JAR, drop it in `~/.ino/extensions/`, and the registry picks it up at startup.

## Module map

```mermaid
flowchart LR
    DSL[ino-dsl<br/>declarative types]
    CORE[ino-core<br/>engine + registries<br/>+ persistence + API]
    TEST[ino-test<br/>fixtures]
    PA[ino-providers-anthropic]
    PO[ino-providers-openai]
    POL[ino-providers-ollama]
    TOOLS[ino-tools-builtin]
    DASH[ino-dashboard]

    DSL --> CORE
    DSL --> PA
    DSL --> PO
    DSL --> POL
    DSL --> TOOLS
    DSL --> TEST

    PA -.ServiceLoader.-> CORE
    PO -.ServiceLoader.-> CORE
    POL -.ServiceLoader.-> CORE
    TOOLS -.ServiceLoader.-> CORE

    DASH -.HTTP/SSE.-> CORE
    TEST --> CORE
```

| Module | Role | Status |
|---|---|---|
| `ino-dsl` | Pure data — `Agent`, `Tool`, `LlmProviderConfig` etc. via konstellation | Built |
| `ino-core` | Spring Boot 4.1 — engine, registries, SQLite persistence, REST + SSE | Planned |
| `ino-test` | `InMemoryLlmProvider`, `INO_HOME` JUnit extension, fixture helpers | Planned |
| `ino-providers-anthropic` | Anthropic Messages API adapter; registers as `LlmProvider` | Planned |
| `ino-providers-openai` | OpenAI Chat/Responses adapter | Planned |
| `ino-providers-ollama` | Ollama HTTP adapter (local) | Planned |
| `ino-tools-builtin` | `shell`, `http_get`, `file_read`, `file_write`, `web_search` tools | Planned |
| `ino-dashboard` | SvelteKit + TypeScript + Tailwind v4 + Storybook 8 + Playwright | Planned |

## ServiceLoader extension model

```mermaid
flowchart TB
    subgraph Boot[ino-core boot sequence]
        SPRING[Spring Boot starts]
        SCAN[Scan classpath + ~/.ino/extensions/*.jar]
        LP_REG[ProviderRegistry loads<br/>ServiceLoader&lt;LlmProvider&gt;]
        TH_REG[ToolRegistry loads<br/>ServiceLoader&lt;ToolHandler&gt;]
        READY[Engine ready]
    end

    subgraph ExtJars[Extension JARs]
        E1["META-INF/services/<br/>org.khorum.oss.ino.core.provider.LlmProvider"]
        E2["META-INF/services/<br/>org.khorum.oss.ino.core.tool.ToolHandler"]
    end

    SPRING --> SCAN
    SCAN --> LP_REG
    SCAN --> TH_REG
    LP_REG --> READY
    TH_REG --> READY

    E1 -.discovered by.-> LP_REG
    E2 -.discovered by.-> TH_REG
```

Each extension JAR is a regular Gradle subproject, shadow-built with `org/khorum/oss/ino/dsl/**` excluded (the DSL is provided by core to avoid duplicate classloading).

## End-to-end request flow

```mermaid
sequenceDiagram
    autonumber
    participant U as User (browser)
    participant DASH as ino-dashboard
    participant API as ino-core REST/SSE
    participant ENG as AgentExecutor
    participant PR as ProviderRegistry
    participant LLM as LlmProvider impl<br/>(extension JAR)
    participant TR as ToolRegistry
    participant TH as ToolHandler impl<br/>(extension JAR)
    participant DB as SQLite (state.db)

    U->>DASH: type message
    DASH->>API: POST /sessions/{id}/messages
    API->>DB: persist user message
    API->>ENG: run(session, message)
    ENG->>PR: resolve(agent.provider.selected)
    PR-->>ENG: LlmProvider impl
    ENG->>LLM: stream(CompletionRequest)
    LLM-->>ENG: CompletionEvent stream
    ENG-->>API: forward as SSE
    API-->>DASH: SSE: TextDelta, ToolCallStart...
    LLM-->>ENG: CompletionEvent.End(TOOL_USE)
    ENG->>TR: lookup(toolName)
    TR-->>ENG: ToolHandler impl
    ENG->>TH: invoke(args)
    TH-->>ENG: ToolResult
    ENG->>DB: persist tool_invocation row
    ENG->>LLM: stream(messages + tool result)
    LLM-->>ENG: ...
    LLM-->>ENG: CompletionEvent.End(END_TURN)
    ENG->>DB: persist final assistant message
    API-->>DASH: SSE: complete
    DASH-->>U: render
```

## Build & runtime contract

- Each Gradle subproject under `ino/` is a submodule of a single Gradle root at `khorum/agents/ino/` (the tabs pattern). Build via `./gradlew :ino-dsl:build`, `./gradlew :ino-core:bootRun`, etc.
- Each subproject has its own `VERSION` file; bumps are independent.
- Provider/tool extension JARs publish to DigitalOcean Spaces under `org.khorum.oss.ino:<artifact>:<version>`.
- Production deployment: a single `ino-core` boot JAR with built-in providers/tools on the classpath, plus the bundled SvelteKit static assets at `/dashboard/**`.

See [cicd.md](cicd.md) for the publishing flow and [roadmap.md](roadmap.md) for what gets built when.
