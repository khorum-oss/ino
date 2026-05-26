import type { AppendMessageRequest, StreamEvent } from './types';

/**
 * Send a user message and consume the SSE stream from
 * `POST /api/sessions/{id}/messages` with `Accept: text/event-stream`.
 *
 * Browser-native `EventSource` is GET-only, so we drive the SSE wire format
 * ourselves on top of `fetch` + a `ReadableStream` reader. The Spring Boot
 * controller emits frames in the standard form:
 *
 *     event: text-delta
 *     data: {"type":"text-delta","text":"hello"}
 *
 *     event: end
 *     data: {"type":"end","stopReason":"end_turn","inputTokens":12,"outputTokens":7}
 *
 * Use as:
 *     for await (const event of streamMessage(sessionId, { content })) {
 *         // event.type === 'text-delta' | 'tool-call-args-delta' | 'end' | ...
 *     }
 *
 * Pass an `AbortSignal` to cancel — this aborts the fetch which closes the
 * SSE connection cleanly. To also cancel the server-side run (so partial
 * state is persisted and tokens stop being charged), call
 * `sessionsApi.cancelRun(sessionId)` in addition.
 */
export async function* streamMessage(
    sessionId: string,
    request: AppendMessageRequest,
    options: { signal?: AbortSignal } = {},
): AsyncGenerator<StreamEvent, void, void> {
    const res = await fetch(`/api/sessions/${encodeURIComponent(sessionId)}/messages`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
        },
        body: JSON.stringify(request),
        signal: options.signal,
    });

    if (!res.ok) {
        const body = await res.text().catch(() => '');
        throw new Error(`[${res.status}] ${res.statusText} :: ${body}`);
    }
    if (!res.body) {
        throw new Error('SSE response has no body');
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    try {
        while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });

            // SSE frames are separated by a blank line ("\n\n" canonically, "\r\n\r\n" allowed).
            let separator: number;
            while ((separator = findFrameBoundary(buffer)) >= 0) {
                const raw = buffer.slice(0, separator);
                buffer = buffer.slice(separator + (buffer[separator] === '\r' ? 4 : 2));
                const event = parseFrame(raw);
                if (event) yield event;
            }
        }
    } finally {
        try {
            await reader.cancel();
        } catch {
            /* ignore */
        }
    }
}

function findFrameBoundary(s: string): number {
    const idx1 = s.indexOf('\n\n');
    const idx2 = s.indexOf('\r\n\r\n');
    if (idx1 === -1) return idx2;
    if (idx2 === -1) return idx1;
    return Math.min(idx1, idx2);
}

function parseFrame(raw: string): StreamEvent | null {
    const lines = raw.split(/\r?\n/);
    let dataLines: string[] = [];
    for (const line of lines) {
        if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trimStart());
        }
        // We could parse `event:` here for the type discriminator, but the
        // payload already carries `type`, so we don't need to.
    }
    if (dataLines.length === 0) return null;
    const json = dataLines.join('\n');
    try {
        return JSON.parse(json) as StreamEvent;
    } catch {
        // Malformed frame — skip rather than crash the consumer.
        return null;
    }
}
