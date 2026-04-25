CREATE TABLE creatures (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) UNIQUE NOT NULL,
    wiki_url            VARCHAR(500),
    image_url           VARCHAR(500),
    hp                  INTEGER,
    experience          INTEGER,
    physical_resistance INTEGER,
    fire_resistance     INTEGER,
    ice_resistance      INTEGER,
    energy_resistance   INTEGER,
    earth_resistance    INTEGER,
    death_resistance    INTEGER,
    holy_resistance     INTEGER,
    drown_resistance    INTEGER,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);