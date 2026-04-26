-- Hunt sessions are import-only records, always complete.
-- Drop the manual lifecycle column, remove the auto-timestamp default
-- from started_at, make ended_at required, and add all analytics columns.

UPDATE hunt_sessions SET ended_at = started_at WHERE ended_at IS NULL;

ALTER TABLE hunt_sessions
    DROP COLUMN  status,
    ALTER COLUMN started_at   DROP DEFAULT,
    ALTER COLUMN ended_at     SET NOT NULL,
    ADD COLUMN   duration     VARCHAR(50) NOT NULL DEFAULT '',
    ADD COLUMN   raw_xp       INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN   xp_with_bonus INTEGER    NOT NULL DEFAULT 0,
    ADD COLUMN   loot_total   INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN   supplies     INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN   damage       INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN   healing      INTEGER     NOT NULL DEFAULT 0;
