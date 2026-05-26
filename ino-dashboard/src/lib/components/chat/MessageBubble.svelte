<script lang="ts">
    import type { MessageRole } from '$lib/api/types';

    interface Props {
        role: MessageRole;
        content: string | null;
        streaming?: boolean;
    }
    let { role, content, streaming = false }: Props = $props();

    const roleStyles: Record<MessageRole, { label: string; color: string; align: string }> = {
        user: {
            label: 'USER',
            color: 'border-[var(--color-neon-pink)]/60 text-[var(--color-fg)]',
            align: 'ml-auto',
        },
        assistant: {
            label: 'ASSISTANT',
            color: 'border-[var(--color-neon-cyan)]/60 text-[var(--color-fg)]',
            align: 'mr-auto',
        },
        system: {
            label: 'SYSTEM',
            color: 'border-[var(--color-border)] text-[var(--color-fg-muted)]',
            align: 'mx-auto',
        },
        tool: {
            label: 'TOOL',
            color: 'border-[var(--color-neon-lime)]/60 text-[var(--color-fg-muted)]',
            align: 'mr-auto',
        },
    };
    const s = $derived(roleStyles[role]);
</script>

<article class="w-full max-w-3xl {s.align}">
    <div class="flex items-center gap-2 text-xs font-mono uppercase tracking-[0.2em] text-[var(--color-fg-dim)] mb-1">
        <span class="text-[var(--color-fg-muted)]">»</span>
        <span>{s.label}</span>
    </div>
    <div class="border-l-2 pl-4 py-2 bg-[var(--color-bg-soft)]/40 {s.color}">
        <p class="font-mono text-sm whitespace-pre-wrap break-words leading-relaxed">
            {content ?? ''}{#if streaming}<span class="cursor-blink text-[var(--color-neon-pink)]">▮</span>{/if}
        </p>
    </div>
</article>
