-- Game worlds are imported from the official world list (via the TibiaData API).
-- Worlds that disappear from the list (merged or closed) are kept, because characters
-- may still reference them, and flagged as inactive instead.
ALTER TABLE servers ADD COLUMN location   VARCHAR(50);
ALTER TABLE servers ADD COLUMN active     BOOLEAN     NOT NULL DEFAULT TRUE;
ALTER TABLE servers ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
