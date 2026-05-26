<script lang="ts">
    import { goto } from '$app/navigation';
    import { base } from '$app/paths';
    import { agentsApi, sessionsApi } from '$lib/api/client';
    import type { AgentSummaryDto } from '$lib/api/types';
    import AgentBadge from '$lib/components/registry/AgentBadge.svelte';
    import NeonButton from '$lib/components/ui/NeonButton.svelte';
    import TerminalCard from '$lib/components/ui/TerminalCard.svelte';

    let agents = $state<AgentSummaryDto[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let creating = $state<string | null>(null);

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

    async function startSession(name: string) {
        creating = name;
        try {
            const session = await sessionsApi.create({ agent: name });
            // Prepend SvelteKit base ('/dashboard' in production, '' in dev)
            // so the browser navigates to /dashboard/sessions/X, not /sessions/X.
            await goto(`${base}/sessions/${session.id}`);
        } catch (e) {
            error = e instanceof Error ? e.message : String(e);
            creating = null;
        }
    }
</script>

<div class="flex flex-col gap-6">
    <div class="flex flex-col gap-2">
        <h1 class="font-mono text-3xl tracking-[0.15em] text-[var(--color-fg)]">
            <span class="text-[var(--color-neon-pink)] text-glow-pink">ino</span><span class="text-[var(--color-fg-dim)]">::</span>home
        </h1>
        <p class="font-mono text-sm text-[var(--color-fg-muted)] max-w-2xl">
            Pick an agent to start a session. Sessions persist to <code class="text-[var(--color-neon-cyan)]">~/.ino/state.db</code> and stream
            responses live via SSE.
        </p>
    </div>

    {#if loading}
        <TerminalCard title="loading">
            <p class="font-mono text-sm text-[var(--color-fg-muted)]">
                <span class="cursor-blink text-[var(--color-neon-cyan)]">▮</span> fetching agents…
            </p>
        </TerminalCard>
    {:else if error}
        <TerminalCard title="error" accent="pink">
            <p class="font-mono text-sm text-[var(--color-danger)] whitespace-pre-wrap">
                {error}
            </p>
            <p class="font-mono text-xs text-[var(--color-fg-dim)] mt-2">
                Is <code class="text-[var(--color-neon-cyan)]">./gradlew :ino-core:bootRun</code> running on port 8080?
            </p>
        </TerminalCard>
    {:else if agents.length === 0}
        <TerminalCard title="no agents">
            <p class="font-mono text-sm text-[var(--color-fg-muted)]">
                The registry is empty. Add agent beans in <code class="text-[var(--color-neon-cyan)]">SampleAgentsConfig.kt</code>.
            </p>
        </TerminalCard>
    {:else}
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            {#each agents as agent (agent.name)}
                <TerminalCard title={agent.name} accent="cyan">
                    {#snippet actions()}
                        <AgentBadge providerId={agent.providerId} model={agent.model} />
                    {/snippet}
                    <div class="flex flex-col gap-4">
                        <p class="font-mono text-sm text-[var(--color-fg-muted)] min-h-[2lh]">
                            {agent.description || '// no description'}
                        </p>
                        <div class="flex justify-end">
                            <NeonButton
                                variant="accent"
                                disabled={creating === agent.name}
                                onclick={() => startSession(agent.name)}
                            >
                                {creating === agent.name ? 'starting…' : 'new session'}
                            </NeonButton>
                        </div>
                    </div>
                </TerminalCard>
            {/each}
        </div>
    {/if}
</div>
