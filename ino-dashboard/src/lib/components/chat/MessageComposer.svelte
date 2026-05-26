<script lang="ts">
    import NeonButton from '$lib/components/ui/NeonButton.svelte';

    interface Props {
        disabled?: boolean;
        streaming?: boolean;
        onsend: (content: string) => void;
        oncancel?: () => void;
    }
    let { disabled = false, streaming = false, onsend, oncancel }: Props = $props();

    let value = $state('');
    let textareaEl: HTMLTextAreaElement | undefined = $state();

    function handleSend() {
        const trimmed = value.trim();
        if (!trimmed || disabled || streaming) return;
        onsend(trimmed);
        value = '';
        textareaEl?.focus();
    }

    function handleKey(e: KeyboardEvent) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    }
</script>

<form
    class="flex flex-col gap-2"
    onsubmit={(e) => {
        e.preventDefault();
        handleSend();
    }}
>
    <div class="relative">
        <textarea
            bind:this={textareaEl}
            bind:value
            onkeydown={handleKey}
            placeholder="// type a message — Enter to send, Shift+Enter for newline"
            disabled={disabled || streaming}
            rows={3}
            class="
                w-full px-3 py-2
                font-mono text-sm leading-relaxed
                bg-[var(--color-bg-input)]
                border border-[var(--color-border)]
                text-[var(--color-fg)] placeholder:text-[var(--color-fg-dim)]
                focus:border-[var(--color-neon-pink)]
                focus:outline-none
                disabled:opacity-50 disabled:cursor-not-allowed
                resize-y min-h-[5rem]
            "
        ></textarea>
        <span
            class="absolute top-2 right-3 text-[10px] font-mono uppercase tracking-[0.2em] text-[var(--color-fg-dim)] pointer-events-none"
        >
            {value.length}c
        </span>
    </div>
    <div class="flex items-center justify-between">
        <span class="text-xs font-mono text-[var(--color-fg-dim)]">
            {#if streaming}
                <span class="text-[var(--color-neon-cyan)]">●</span> streaming…
            {:else}
                <span class="text-[var(--color-fg-dim)]">○</span> ready
            {/if}
        </span>
        <div class="flex items-center gap-2">
            {#if streaming && oncancel}
                <NeonButton variant="danger" onclick={oncancel}>cancel</NeonButton>
            {/if}
            <NeonButton variant="accent" type="submit" disabled={disabled || streaming || value.trim().length === 0}>
                send
            </NeonButton>
        </div>
    </div>
</form>
