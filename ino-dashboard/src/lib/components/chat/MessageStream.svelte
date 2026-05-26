<script lang="ts">
    import type { MessageDto, MessageRole } from '$lib/api/types';
    import MessageBubble from './MessageBubble.svelte';

    /**
     * Renders the persisted history plus an optional in-flight assistant
     * message currently streaming. The parent owns the streaming buffer and
     * passes it as `streamingContent`.
     */
    interface Props {
        messages: MessageDto[];
        streamingContent?: string;
        streaming?: boolean;
    }
    let { messages, streamingContent = '', streaming = false }: Props = $props();
</script>

<div class="flex flex-col gap-5">
    {#each messages as msg (msg.id)}
        <MessageBubble role={msg.role} content={msg.content} />
    {/each}

    {#if streaming || streamingContent.length > 0}
        <MessageBubble
            role={'assistant' satisfies MessageRole}
            content={streamingContent}
            streaming={streaming}
        />
    {/if}
</div>
