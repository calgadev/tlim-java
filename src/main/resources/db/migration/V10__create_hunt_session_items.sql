CREATE TABLE hunt_session_items (
    id              BIGSERIAL PRIMARY KEY,
    hunt_session_id BIGINT  NOT NULL REFERENCES hunt_sessions(id) ON DELETE CASCADE,
    item_id         BIGINT  NOT NULL REFERENCES items(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uq_hunt_session_item UNIQUE (hunt_session_id, item_id)
);