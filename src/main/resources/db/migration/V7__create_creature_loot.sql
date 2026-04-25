CREATE TABLE creature_loot (
    id          BIGSERIAL PRIMARY KEY,
    creature_id BIGINT      NOT NULL REFERENCES creatures(id) ON DELETE CASCADE,
    item_id     BIGINT      NOT NULL REFERENCES items(id)    ON DELETE CASCADE,
    rarity      VARCHAR(50),
    min_amount  INTEGER     NOT NULL DEFAULT 1,
    max_amount  INTEGER     NOT NULL DEFAULT 1,
    CONSTRAINT uq_creature_loot_creature_item UNIQUE (creature_id, item_id)
);