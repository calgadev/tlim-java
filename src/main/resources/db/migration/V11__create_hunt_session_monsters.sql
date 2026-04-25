CREATE TABLE hunt_session_monsters (
    id              BIGSERIAL PRIMARY KEY,
    hunt_session_id BIGINT  NOT NULL REFERENCES hunt_sessions(id) ON DELETE CASCADE,
    creature_id     BIGINT  NOT NULL REFERENCES creatures(id),
    kill_count      INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uq_hunt_session_monster UNIQUE (hunt_session_id, creature_id)
);