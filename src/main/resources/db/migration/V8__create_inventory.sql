CREATE TABLE inventory (
    id               BIGSERIAL PRIMARY KEY,
    character_id     BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    item_id          BIGINT      NOT NULL REFERENCES items(id),
    current_quantity INTEGER     NOT NULL DEFAULT 0,
    target_quantity  INTEGER     NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_character_item UNIQUE (character_id, item_id)
);