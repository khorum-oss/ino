<script lang="ts">
    import { page } from '$app/state';
    import { sessionsApi } from '$lib/api/client';
    import { streamMessage } from '$lib/api/sse';
    import type { MessageDto, SessionDto, StreamEvent } from '$lib/api/types';
    import AgentBadge from '$lib/components/registry/AgentBadge.svelte';
    import MessageComposer from '$lib/components/chat/MessageComposer.svelte';
    import MessageStream from '$lib/components/chat/MessageStream.svelte';
    import TerminalCard from '$lib/components/ui/TerminalCard.svelte';

    // page.params.id is typed `string | undefined`; route guarantees it at runtime.
    const sessionId = $derived(page.params.id as string);

    let session = $state<SessionDto | null>(null);
    let messages = $state<MessageDto[]>([]);
    let streamingBuffer = $state('');
    let streaming = $state(false);
    let error = $state<string | null>(null);
    let abortCtl: AbortController | null = $state(null);

    $effect(() => {
        // Re-load when the session id in the URL changes.
        const id = sessionId;
        Promise.all([sessionsApi.get(id), sessionsApi.history(id)])
            .then(([s, h]) => {
                session = s;
                messages = h;
            })
            .catch((e: unknown) => {
                error = e instanceof Error ? e.message : String(e);
            });
    });

    async function send(content: string) {
        if (streaming || !sessionId) return;
        error = null;
        streaming = true;
        streamingBuffer = '';

        // Optimistic user-message append. The server persists it too; the
        // refresh after stream completion will reconcile ordering.
        messages = [
            ...messages,
            {
                id: `optimistic-${Date.now()}`,
                sessionId,
                seq: messages.length,
                role: 'user',
                content,
                reasoning: null,
                toolCallId: null,
                toolCalls: null,
                inputTokens: null,
                outputTokens: null,
                createdAt: new Date().toISOString(),
            },
        ];

        abortCtl = new AbortController();
        try {
            for await (const ev of streamMessage(sessionId, { content }, { signal: abortCtl.signal })) {
                handleEvent(ev);
            }
        } catch (e) {
            if ((e as Error).name !== 'AbortError') {
                error = (e as Error).message;
            }
        } finally {
            streaming = false;
            abortCtl = null;
            // Reconcile with the server's canonical message order.
            try {
                messages = await sessionsApi.history(sessionId);
            } catch {
                /* leave optimistic state */
            }
            streamingBuffer = '';
        }
    }

    function handleEvent(ev: StreamEvent) {
        switch (ev.type) {
            case 'text-delta':
                if (ev.text) streamingBuffer += ev.text;
                break;
            case 'reasoning-delta':
                // Surface reasoning later in its own panel; ignore for MVP.
                break;
            case 'tool-call-start':
            case 'tool-call-args-delta':
            case 'tool-call-end':
                // Tool dispatch is phase 1.5 — log for now.
                console.debug('[ino] tool-call event', ev);
                break;
            case 'end':
                console.debug('[ino] stream end', ev);
                break;
            case 'error':
                error = ev.text ?? 'stream error';
                break;
        }
    }

    async function cancel() {
        if (!streaming) return;
        try {
            await sessionsApi.cancelRun(sessionId);
        } catch (e) {
            // The DELETE may race with stream completion — that's fine.
            console.debug('[ino] cancel race', e);
        }
        abortCtl?.abort();
    }
</script>

<div class="flex flex-col gap-6">
    <header class="flex items-baseline justify-between gap-4">
        <div class="flex flex-col gap-1">
            <h1 class="font-mono text-2xl tracking-[0.15em]">
                {#if session}
                    <span class="text-[var(--color-neon-cyan)] text-glow-cyan">{session.agentName}</span>
                    <span class="text-[var(--color-fg-dim)] text-sm">// session {session.id.slice(0, 8)}</span>
                {:else}
                    <span class="cursor-blink">▮</span> loading…
                {/if}
            </h1>
        </div>
        {#if session}
            <AgentBadge providerId={session.providerId} model={session.model} />
        {/if}
    </header>

    {#if error}
        <TerminalCard title="error" accent="pink">
            <p class="font-mono text-sm text-[var(--color-danger)] whitespace-pre-wrap">{error}</p>
        </TerminalCard>
    {/if}

    <TerminalCard title="conversation" accent="cyan">
        <div class="flex flex-col gap-5 max-h-[60vh] overflow-y-auto pr-2">
            {#if messages.length === 0 && !streaming}
                <p class="font-mono text-sm text-[var(--color-fg-dim)] text-center py-8">
                    // empty session — type a message below to start
                </p>
            {:else}
                <MessageStream
                    messages={messages}
                    streamingContent={streamingBuffer}
                    streaming={streaming}
                />
            {/if}
        </div>
    </TerminalCard>

    <TerminalCard title="composer">
        <MessageComposer
            disabled={!session}
            streaming={streaming}
            onsend={send}
            oncancel={cancel}
        />
    </TerminalCard>
</div>
