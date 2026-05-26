<script lang="ts">
    import '../app.css';
    import { page } from '$app/state';

    interface Props {
        children?: import('svelte').Snippet;
    }
    let { children }: Props = $props();

    const navItems = [
        { href: '/', label: 'home' },
        { href: '/agents', label: 'agents' },
    ];
</script>

<div class="relative z-10 min-h-screen flex flex-col">
    <header class="border-b border-[var(--color-border)] bg-[var(--color-bg)]/70 backdrop-blur-md">
        <div class="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
            <a href="/" class="group flex items-center gap-3">
                <span class="text-[var(--color-neon-pink)] text-glow-pink font-mono text-2xl font-bold tracking-[0.25em]">
                    ino
                </span>
                <span class="text-[var(--color-fg-dim)] font-mono text-xs uppercase tracking-[0.3em]">
                    /// agent runtime
                </span>
            </a>
            <nav class="flex items-center gap-6">
                {#each navItems as item}
                    {@const active = page.url.pathname === item.href || (item.href !== '/' && page.url.pathname.startsWith(item.href))}
                    <a
                        href={item.href}
                        class="font-mono text-xs uppercase tracking-[0.25em] transition-colors {active
                            ? 'text-[var(--color-neon-cyan)] text-glow-cyan'
                            : 'text-[var(--color-fg-muted)] hover:text-[var(--color-fg)]'}"
                    >
                        {item.label}
                    </a>
                {/each}
            </nav>
        </div>
    </header>

    <main class="flex-1 max-w-6xl w-full mx-auto px-6 py-8">
        {@render children?.()}
    </main>

    <footer class="border-t border-[var(--color-border)] py-3 text-center">
        <span class="font-mono text-[10px] uppercase tracking-[0.3em] text-[var(--color-fg-dim)]">
            ino // koog runtime // ready
        </span>
    </footer>
</div>
