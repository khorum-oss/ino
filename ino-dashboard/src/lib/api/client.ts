import type {
    AgentDetailsDto,
    AgentSummaryDto,
    AppendMessageRequest,
    CreateSessionRequest,
    MessageDto,
    ProviderTypeDto,
    SessionDto,
    ToolSummaryDto,
} from './types';

/**
 * Minimal typed wrapper over ino-core's REST surface. The vite dev server
 * proxies `/api/*` to the Spring Boot app on port 8080 (see vite.config.ts);
 * in production the dashboard is served from `/dashboard/**` by ino-core
 * itself, so same-origin requests work without proxying.
 */

const BASE = '/api';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const res = await fetch(`${BASE}${path}`, {
        ...init,
        headers: {
            'Content-Type': 'application/json',
            ...(init.headers ?? {}),
        },
    });
    if (!res.ok) {
        const body = await res.text().catch(() => '');
        throw new Error(`[${res.status}] ${res.statusText} :: ${path} :: ${body}`);
    }
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
}

/* ---------------------------------------------------------------------------
   Sessions
   --------------------------------------------------------------------------- */

export const sessionsApi = {
    create(req: CreateSessionRequest): Promise<SessionDto> {
        return request<SessionDto>('/sessions', {
            method: 'POST',
            body: JSON.stringify(req),
        });
    },
    get(id: string): Promise<SessionDto> {
        return request<SessionDto>(`/sessions/${encodeURIComponent(id)}`);
    },
    list(opts: { agent: string; limit?: number; offset?: number }): Promise<SessionDto[]> {
        const params = new URLSearchParams({ agent: opts.agent });
        if (opts.limit) params.set('limit', String(opts.limit));
        if (opts.offset) params.set('offset', String(opts.offset));
        return request<SessionDto[]>(`/sessions?${params.toString()}`);
    },
    history(sessionId: string): Promise<MessageDto[]> {
        return request<MessageDto[]>(`/sessions/${encodeURIComponent(sessionId)}/messages`);
    },
    cancelRun(sessionId: string): Promise<void> {
        return request<void>(`/sessions/${encodeURIComponent(sessionId)}/run`, {
            method: 'DELETE',
        });
    },
    sendSync(sessionId: string, req: AppendMessageRequest): Promise<MessageDto> {
        return request<MessageDto>(`/sessions/${encodeURIComponent(sessionId)}/messages`, {
            method: 'POST',
            body: JSON.stringify(req),
        });
    },
};

/* ---------------------------------------------------------------------------
   Agents / Providers / Tools (read-only introspection)
   --------------------------------------------------------------------------- */

export const agentsApi = {
    list(): Promise<AgentSummaryDto[]> {
        return request<AgentSummaryDto[]>('/agents');
    },
    get(name: string): Promise<AgentDetailsDto> {
        return request<AgentDetailsDto>(`/agents/${encodeURIComponent(name)}`);
    },
};

export const providersApi = {
    list(): Promise<ProviderTypeDto[]> {
        return request<ProviderTypeDto[]>('/providers');
    },
};

export const toolsApi = {
    list(): Promise<ToolSummaryDto[]> {
        return request<ToolSummaryDto[]>('/tools');
    },
};
