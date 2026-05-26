<script lang="ts">
    import { page } from '$app/state';
    import { agentsApi } from '$lib/api/client';
    import type { AgentDetailsDto } from '$lib/api/types';
    import AgentBadge from '$lib/components/registry/AgentBadge.svelte';
    import TerminalCard from '$lib/components/ui/TerminalCard.svelte';

    // page.params.name is typed as `string | undefined` even when the route file
    // is `[name]/+page.svelte` — SvelteKit can't statically prove the param is present.
    // We know it is at runtime, so assert.
    const name = $derived(page.params.name as string);
    let agent = $state<AgentDetailsDto | null>(null);
    let error = $state<string | null>(null);

    $effect(() => {
        const n = name;
        if (!n) return;
        agentsApi
            .get(n)
            .then((a) => (agent = a))
            .catch((e: unknown) => (error = e instanceof Error ? e.message : String(e)));
    });
</script>

<div class="flex flex-col gap-6">
    {#if error}
        <TerminalCard title="error" accent="pink">
            <p class="font-mono text-sm text-[var(--color-danger)]">{error}</p>
        </TerminalCard>
    {:else if !agent}
        <p class="font-mono text-sm text-[var(--color-fg-muted)]">loading…</p>
    {:else}
        <header class="flex items-baseline justify-between gap-4">
            <h1 class="font-mono text-2xl tracking-[0.15em] text-[var(--color-neon-cyan)] text-glow-cyan">
                {agent.name}
            </h1>
            <AgentBadge providerId={agent.providerId} model={agent.model} />
        </header>

        <TerminalCard title="description">
            <p class="font-mono text-sm text-[var(--color-fg-muted)] whitespace-pre-wrap">
                {agent.description || '// no description'}
            </p>
        </TerminalCard>

        <TerminalCard title="system prompt">
            <pre class="font-mono text-xs text-[var(--color-fg)] whitespace-pre-wrap overflow-x-auto">{agent.systemPrompt || '// none'}</pre>
        </TerminalCard>

        <TerminalCard title="configuration">
            <dl class="grid grid-cols-2 gap-4 font-mono text-xs">
                <div>
                    <dt class="text-[var(--color-fg-dim)] uppercase tracking-[0.2em]">max iterations</dt>
                    <dd class="text-[var(--color-fg)] mt-1">{agent.maxIterations}</dd>
                </div>
                <div>
                    <dt class="text-[var(--color-fg-dim)] uppercase tracking-[0.2em]">budget (usd micros)</dt>
                    <dd class="text-[var(--color-fg)] mt-1">{agent.budgetUsdMicros ?? '∞'}</dd>
                </div>
            </dl>
        </TerminalCard>

        <TerminalCard title="tools" accent={agent.tools.length === 0 ? 'cyan' : 'pink'}>
            {#if agent.tools.length === 0}
                <p class="font-mono text-xs text-[var(--color-fg-dim)]">// none registered</p>
            {:else}
                <ul class="flex flex-col gap-2">
                    {#each agent.tools as tool (tool.name)}
                        <li class="border-l border-[var(--color-border)] pl-3">
                            <span class="font-mono text-sm text-[var(--color-neon-pink)]">{tool.name}</span>
                            <span class="font-mono text-xs text-[var(--color-fg-dim)]"> — {tool.description}</span>
                        </li>
                    {/each}
                </ul>
            {/if}
        </TerminalCard>
    {/if}
</div>
