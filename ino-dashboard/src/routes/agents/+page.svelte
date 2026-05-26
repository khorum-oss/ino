<script lang="ts">
    import { agentsApi } from '$lib/api/client';
    import type { AgentSummaryDto } from '$lib/api/types';
    import AgentBadge from '$lib/components/registry/AgentBadge.svelte';
    import TerminalCard from '$lib/components/ui/TerminalCard.svelte';

    let agents = $state<AgentSummaryDto[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);

    $effect(() => {
        agentsApi
            .list()
            .then((list) => {
                agents = list;
                loading = false;
            })
            .catch((e: unknown) => {
                error = e instanceof Error ? e.message : String(e);
                loading = false;
            });
    });
</script>

<div class="flex flex-col gap-6">
    <h1 class="font-mono text-2xl tracking-[0.15em]">
        <span class="text-[var(--color-neon-cyan)] text-glow-cyan">agents</span>
        <span class="text-[var(--color-fg-dim)] text-sm">// {agents.length} registered</span>
    </h1>

    {#if loading}
        <TerminalCard><p class="font-mono text-sm text-[var(--color-fg-muted)]">loading…</p></TerminalCard>
    {:else if error}
        <TerminalCard title="error" accent="pink">
            <p class="font-mono text-sm text-[var(--color-danger)]">{error}</p>
        </TerminalCard>
    {:else}
        <div class="grid grid-cols-1 gap-3">
            {#each agents as agent (agent.name)}
                <a href={`/agents/${agent.name}`} class="block link-neon !border-b-0">
                    <TerminalCard accent="cyan">
                        <div class="flex items-center justify-between gap-4">
                            <div class="flex flex-col gap-1">
                                <h2 class="font-mono text-base text-[var(--color-fg)] tracking-wider">
                                    {agent.name}
                                </h2>
                                <p class="font-mono text-xs text-[var(--color-fg-muted)]">
                                    {agent.description || '// no description'}
                                </p>
                            </div>
                            <AgentBadge providerId={agent.providerId} model={agent.model} />
                        </div>
                    </TerminalCard>
                </a>
            {/each}
        </div>
    {/if}
</div>
