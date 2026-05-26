<script lang="ts">
    /**
     * Primary action button in the cyberpunk palette. Variants tint the
     * border + glow color; `accent` is the canonical hot-magenta call-to-action.
     */
    type Variant = 'accent' | 'cyan' | 'ghost' | 'danger';

    interface Props {
        variant?: Variant;
        type?: 'button' | 'submit' | 'reset';
        disabled?: boolean;
        onclick?: (e: MouseEvent) => void;
        children?: import('svelte').Snippet;
    }

    let {
        variant = 'accent',
        type = 'button',
        disabled = false,
        onclick,
        children,
    }: Props = $props();

    const variantClass: Record<Variant, string> = {
        accent:
            'border-[var(--color-neon-pink)] text-[var(--color-neon-pink)] hover:bg-[oklch(0.74_0.32_350/0.12)] hover:shadow-[var(--shadow-neon-pink)]',
        cyan:
            'border-[var(--color-neon-cyan)] text-[var(--color-neon-cyan)] hover:bg-[oklch(0.85_0.18_215/0.12)] hover:shadow-[var(--shadow-neon-cyan)]',
        ghost:
            'border-[var(--color-border)] text-[var(--color-fg-muted)] hover:text-[var(--color-fg)] hover:border-[var(--color-border-bright)]',
        danger:
            'border-[var(--color-danger)] text-[var(--color-danger)] hover:bg-[oklch(0.68_0.27_25/0.12)]',
    };
</script>

<button
    {type}
    {disabled}
    {onclick}
    class="
        inline-flex items-center gap-2
        font-mono uppercase tracking-wider text-sm
        px-4 py-2
        bg-transparent border
        transition-all duration-150
        cursor-pointer
        disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:shadow-none
        {variantClass[variant]}
    "
>
    {@render children?.()}
</button>
