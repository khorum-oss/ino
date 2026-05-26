/**
 * Wire types matching the DTOs in `ino-core`. Hand-written for MVP; once the
 * REST surface settles, this file will be regenerated from springdoc-openapi
 * via openapi-typescript.
 */

export interface SessionDto {
    id: string;
    agentName: string;
    providerId: string;
    model: string;
    status: 'active' | 'completed' | 'failed' | 'cancelled';
    parentId: string | null;
    metadata: Record<string, unknown>;
    startedAt: string;
    endedAt: string | null;
    totalInputTokens: number;
    totalOutputTokens: number;
    totalCostUsdMicros: number;
}

export type MessageRole = 'system' | 'user' | 'assistant' | 'tool';

export interface MessageDto {
    id: string;
    sessionId: string;
    seq: number;
    role: MessageRole;
    content: string | null;
    reasoning: string | null;
    toolCallId: string | null;
    toolCalls: Array<Record<string, unknown>> | null;
    inputTokens: number | null;
    outputTokens: number | null;
    createdAt: string;
}

export interface AgentSummaryDto {
    name: string;
    description: string;
    providerId: string;
    model: string;
}

export interface AgentDetailsDto extends AgentSummaryDto {
    systemPrompt: string;
    maxIterations: number;
    budgetUsdMicros: number | null;
    tools: ToolSummaryDto[];
}

export interface ToolParameterDto {
    name: string;
    type: string;
    description: string;
    required: boolean;
}

export interface ToolSummaryDto {
    name: string;
    description: string;
    parameters: ToolParameterDto[];
    dangerous: boolean;
    timeoutSeconds: number | null;
    version: string | null;
}

export interface ProviderTypeDto {
    id: string;
    displayName: string;
    supportsBaseUrlOverride: boolean;
    requiresApiKey: boolean;
    implemented: boolean;
    notes: string | null;
}

/* ---------------------------------------------------------------------------
   SSE stream events
   --------------------------------------------------------------------------- */

export type StreamEventType =
    | 'text-delta'
    | 'reasoning-delta'
    | 'tool-call-start'
    | 'tool-call-args-delta'
    | 'tool-call-end'
    | 'end'
    | 'error';

export interface StreamEvent {
    type: StreamEventType;
    text?: string;
    id?: string;
    name?: string;
    argsJsonChunk?: string;
    stopReason?: string;
    inputTokens?: number;
    outputTokens?: number;
}

/* ---------------------------------------------------------------------------
   Requests
   --------------------------------------------------------------------------- */

export interface CreateSessionRequest {
    agent: string;
}

export interface AppendMessageRequest {
    content: string;
}
