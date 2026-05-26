import { describe, expect, it } from 'vitest';
import type { StreamEvent } from '../../src/lib/api/types';

/**
 * Unit tests for the SSE frame parser. We exercise the internal parser by
 * feeding `streamMessage` a fake fetch via globalThis stub. The parser
 * itself lives inline in sse.ts; the goal here is to verify that real-world
 * SSE wire shapes (split across chunks, mixed line endings, blank-line frame
 * separators) are parsed correctly.
 */

import { streamMessage } from '../../src/lib/api/sse';

function chunkedBody(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder();
    return new ReadableStream({
        start(controller) {
            for (const c of chunks) controller.enqueue(encoder.encode(c));
            controller.close();
        },
    });
}

function stubFetch(chunks: string[]) {
    (globalThis as { fetch?: typeof fetch }).fetch = async () =>
        new Response(chunkedBody(chunks), {
            status: 200,
            headers: { 'Content-Type': 'text/event-stream' },
        });
}

describe('streamMessage', () => {
    it('parses well-formed SSE frames into typed events', async () => {
        stubFetch([
            'event: text-delta\n',
            'data: {"type":"text-delta","text":"hello"}\n\n',
            'event: text-delta\n',
            'data: {"type":"text-delta","text":" world"}\n\n',
            'event: end\n',
            'data: {"type":"end","stopReason":"end_turn","inputTokens":12,"outputTokens":2}\n\n',
        ]);

        const events: StreamEvent[] = [];
        for await (const ev of streamMessage('sess-1', { content: 'hi' })) {
            events.push(ev);
        }
        expect(events.map((e) => e.type)).toEqual(['text-delta', 'text-delta', 'end']);
        expect((events[0] as StreamEvent).text).toBe('hello');
        expect((events[2] as StreamEvent).inputTokens).toBe(12);
    });

    it('handles a frame split across multiple reader chunks', async () => {
        stubFetch([
            'event: text-delta\ndata: {"type":"text-de',
            'lta","text":"split"}\n\n',
        ]);

        const events: StreamEvent[] = [];
        for await (const ev of streamMessage('sess-1', { content: 'hi' })) {
            events.push(ev);
        }
        expect(events).toHaveLength(1);
        expect(events[0].text).toBe('split');
    });

    it('skips malformed frames without aborting the stream', async () => {
        stubFetch([
            'data: NOT JSON\n\n',
            'data: {"type":"end","stopReason":"end_turn"}\n\n',
        ]);
        const events: StreamEvent[] = [];
        for await (const ev of streamMessage('sess-1', { content: 'hi' })) {
            events.push(ev);
        }
        expect(events.map((e) => e.type)).toEqual(['end']);
    });
});
