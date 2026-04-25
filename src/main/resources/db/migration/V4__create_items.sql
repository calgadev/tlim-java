CREATE TABLE items (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(255) UNIQUE NOT NULL,
    wiki_url              VARCHAR(500),
    description           TEXT,
    image_url             VARCHAR(500),
    weight                DECIMAL(8,2),
    category              VARCHAR(100),
    is_quest_item         BOOLEAN     NOT NULL DEFAULT FALSE,
    is_imbuement_material BOOLEAN     NOT NULL DEFAULT FALSE,
    is_delivery_item      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);