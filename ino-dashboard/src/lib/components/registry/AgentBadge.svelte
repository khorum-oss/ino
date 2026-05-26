<script lang="ts">
    interface Props {
        providerId: string;
        model: string;
    }
    let { providerId, model }: Props = $props();

    // Provider id determines accent. local/openai/anthropic each get their own.
    const providerColor: Record<string, string> = {
        openai: 'text-[var(--color-neon-cyan)] border-[var(--color-neon-cyan)]/40',
        local: 'text-[var(--color-neon-lime)] border-[var(--color-neon-lime)]/40',
        anthropic: 'text-[var(--color-neon-amber)] border-[var(--color-neon-amber)]/40',
        custom: 'text-[var(--color-fg-muted)] border-[var(--color-border)]',
    };
    // $derived so we react to providerId changes from $props().
    const className = $derived(providerColor[providerId] ?? providerColor.custom);
</script>

<span
    class="inline-flex items-center gap-2 px-2 py-0.5 font-mono text-xs uppercase tracking-wider border {className}"
>
    <span>{providerId}</span>
    <span class="text-[var(--color-fg-dim)]">/</span>
    <span class="text-[var(--color-fg)]">{model}</span>
</span>
