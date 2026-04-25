CREATE TABLE npc_buyers (
    id       BIGSERIAL PRIMARY KEY,
    item_id  BIGINT       NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    npc_name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    price    INTEGER      NOT NULL,
    CONSTRAINT uq_npc_buyer_item_npc UNIQUE (item_id, npc_name)
);