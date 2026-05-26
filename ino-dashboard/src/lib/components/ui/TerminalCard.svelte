<script lang="ts">
    /**
     * Card with the terminal-window vibe: neon-tinted border, faint inner
     * scanline overlay, optional title bar with corner markers.
     */
    interface Props {
        title?: string;
        accent?: 'pink' | 'cyan';
        children?: import('svelte').Snippet;
        actions?: import('svelte').Snippet;
    }
    let { title, accent = 'cyan', children, actions }: Props = $props();

    const borderColor = $derived(
        accent === 'pink' ? 'border-[var(--color-neon-pink)]' : 'border-[var(--color-border-bright)]',
    );
    const titleColor = $derived(
        accent === 'pink' ? 'text-[var(--color-neon-pink)]' : 'text-[var(--color-neon-cyan)]',
    );
</script>

<section
    class="relative bg-[var(--color-bg-card)]/70 backdrop-blur-sm border {borderColor} font-mono"
>
    {#if title}
        <header
            class="flex items-center justify-between gap-3 px-4 py-2 border-b border-[var(--color-border)] bg-[var(--color-bg-soft)]"
        >
            <div class="flex items-center gap-3">
                <span class="text-[var(--color-fg-dim)]">▮</span>
                <h2 class="text-xs uppercase tracking-[0.2em] {titleColor}">
                    {title}
                </h2>
            </div>
            {#if actions}
                <div class="flex items-center gap-2">{@render actions()}</div>
            {/if}
        </header>
    {/if}

    <!-- corner markers -->
    <span class="absolute -top-px -left-px h-2 w-2 border-t border-l {borderColor}"></span>
    <span class="absolute -top-px -right-px h-2 w-2 border-t border-r {borderColor}"></span>
    <span class="absolute -bottom-px -left-px h-2 w-2 border-b border-l {borderColor}"></span>
    <span class="absolute -bottom-px -right-px h-2 w-2 border-b border-r {borderColor}"></span>

    <div class="p-4">
        {@render children?.()}
    </div>
</section>
