CREATE TABLE hunt_sessions (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    location     VARCHAR(255),
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at     TIMESTAMPTZ
);