--liquibase formatted sql

--changeset ino:0001_create_sessions
CREATE TABLE sessions (
    id                    TEXT    PRIMARY KEY NOT NULL,
    agent_name            TEXT    NOT NULL,
    provider_id           TEXT    NOT NULL,
    model                 TEXT    NOT NULL,
    status                TEXT    NOT NULL CHECK (status IN ('active', 'completed', 'failed', 'cancelled')),
    parent_id             TEXT    NULL REFERENCES sessions(id),
    metadata_json         TEXT    NOT NULL DEFAULT '{}',
    started_at            TEXT    NOT NULL,
    ended_at              TEXT    NULL,
    total_input_tokens    INTEGER NOT NULL DEFAULT 0,
    total_output_tokens   INTEGER NOT NULL DEFAULT 0,
    total_cost_usd_micros INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_sessions_agent_started ON sessions(agent_name, started_at DESC);

--changeset ino:0002_create_messages
CREATE TABLE messages (
    id              TEXT    PRIMARY KEY NOT NULL,
    session_id      TEXT    NOT NULL REFERENCES sessions(id),
    seq             INTEGER NOT NULL,
    role            TEXT    NOT NULL CHECK (role IN ('system', 'user', 'assistant', 'tool')),
    content         TEXT    NULL,
    reasoning       TEXT    NULL,
    tool_call_id    TEXT    NULL,
    tool_calls_json TEXT    NULL,
    input_tokens    INTEGER NULL,
    output_tokens   INTEGER NULL,
    created_at      TEXT    NOT NULL,
    UNIQUE (session_id, seq)
);

CREATE INDEX idx_messages_session_seq ON messages(session_id, seq);

--changeset ino:0003_create_tool_invocations
CREATE TABLE tool_invocations (
    id             TEXT    PRIMARY KEY NOT NULL,
    message_id     TEXT    NOT NULL REFERENCES messages(id),
    tool_name      TEXT    NOT NULL,
    arguments_json TEXT    NOT NULL,
    result_json    TEXT    NULL,
    error_json     TEXT    NULL,
    status         TEXT    NOT NULL CHECK (status IN ('pending', 'success', 'error', 'cancelled')),
    started_at     TEXT    NOT NULL,
    finished_at    TEXT    NULL,
    duration_ms    INTEGER NULL
);

CREATE INDEX idx_tool_inv_message ON tool_invocations(message_id);
