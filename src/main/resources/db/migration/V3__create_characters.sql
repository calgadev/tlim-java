CREATE TABLE characters (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    server_id  BIGINT       NOT NULL REFERENCES servers(id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_character_name_server UNIQUE (name, server_id)
);