CREATE TABLE server_item_prices (
    id           BIGSERIAL PRIMARY KEY,
    server_id    BIGINT      NOT NULL REFERENCES servers(id) ON DELETE RESTRICT,
    item_id      BIGINT      NOT NULL REFERENCES items(id)  ON DELETE CASCADE,
    market_price INTEGER,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (server_id, item_id)
);
