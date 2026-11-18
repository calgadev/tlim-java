-- In-game (lowercase) names as used by the Hunt Analyser, taken from the
-- TibiaWiki infobox "actualname" field. Hunt imports match against this column.
ALTER TABLE items     ADD COLUMN actual_name VARCHAR(255);
ALTER TABLE creatures ADD COLUMN actual_name VARCHAR(255);

CREATE INDEX idx_items_actual_name_lower     ON items     (LOWER(actual_name));
CREATE INDEX idx_items_name_lower            ON items     (LOWER(name));
CREATE INDEX idx_creatures_actual_name_lower ON creatures (LOWER(actual_name));
CREATE INDEX idx_creatures_name_lower        ON creatures (LOWER(name));
