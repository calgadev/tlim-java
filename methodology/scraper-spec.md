# Spec — TibiaWiki Scraper Bug Fixes + HTTP Error Observability

## Overview
This Spec covers two targeted bug fixes in `CreatureScraper.java` and one new
cross-cutting behaviour: structured WARN logging for HTTP 403 and 429 responses
encountered during any scrape pass. The logging change is isolated to
`WikiApiClient.java`, which is the single point where all HTTP calls are made.
No schema changes, no new dependencies, and no changes to any entity, DTO, or
controller class are required. The stack is Java 17 / Spring Boot 3.5.0 /
Spring Data JPA / PostgreSQL.

## What is NOT in scope
- Any changes to `ScraperService`, `AdminController`, `ItemScraper`,
  or any entity/DTO class
- Database migrations (no schema changes needed)
- Retry logic (logging only — the human tunes the delay and re-runs)
- Aborting the run on 403/429 (the scraper skips the failing page and continues)
- Item scrape coverage (quest-only or seasonal items in uncovered categories)
- Scrape status persistence across server restarts
- Multi-instance concurrency protection

---

## Phase 1 — Fix elemental resistance field name lookups

**File to modify:** `src/.../scraper/CreatureScraper.java`

Locate the `scrapeCreature()` method, specifically the block that reads
resistance values from the parsed `fields` map and calls `parseResistance()`.

- Replace every wrong short-form key (`"physical"`, `"fire"`, `"ice"`, etc.)
  with the correct camelCase keys as confirmed from the real Dragon wikitext.
  Because `parseFields()` lowercases all keys before storing them, the lookups
  must use the fully lowercased versions:
  - `physicaldmgmod`
  - `firedmgmod`
  - `icedmgmod`
  - `earthdmgmod`
  - `energydmgmod`
  - `deathdmgmod`
  - `holydmgmod`
  - `hpdrainndmgmod`
  - `drowndmgmod`
  - `healmod`
- Confirm the casing by reading `parseFields()` before writing the fix —
  if it does not lowercase, use the original camelCase keys instead.
- Update `parseResistance()` (or its call sites) to strip a trailing `%`
  character before calling `Integer.parseInt()`. The method must handle both
  `"100%"` and `"100"` without throwing.

---

## Phase 2 — Fix loot item name extraction

**File to modify:** `src/.../scraper/CreatureScraper.java`

Locate the `parseLootEntries()` method, specifically the logic that splits
each `{{Loot Item|...|...|...}}` block on `|` and extracts the item name.

The confirmed real format from the Dragon page is positional with two variants:

- With amount:    `{{Loot Item|1-105|Gold Coin|common}}`
- Without amount: `{{Loot Item|Steel Shield|uncommon}}`

- After splitting on `|` and trimming whitespace from each part, inspect
  `parts[1]` (the first real data segment after the template name).
- If `parts[1]` matches an amount pattern — one or more digits, optionally
  followed by a dash and one or more digits (e.g. `1-105`, `3`, `0-1`) —
  then the item name is `parts[2]`.
- If `parts[1]` does not match the amount pattern, the item name is `parts[1]`.
- Trim the extracted item name before passing it to
  `itemRepository.findByName()`.
- The rarity field (last segment) is unaffected by this fix and can be read
  as before.

---

## Phase 3 — Log 403 and 429 HTTP errors and continue

**File to modify:** `src/.../scraper/WikiApiClient.java`

Locate the method(s) that execute HTTP requests and check the response status
code. Currently the code throws a `ScraperException` on any non-200 response.

- Before throwing `ScraperException`, add a conditional check: if the status
  code is 403 or 429, log a `WARN`-level message using the existing SLF4J
  logger (or add one if the class does not have one yet). The message must
  include: the status code, the page title or URL that was being fetched, and
  a human-readable hint for 429 specifically (e.g. `"consider increasing
  tlim.scraper.delay-ms"`).
- After logging, **do not throw** — return an empty `Optional` or a sentinel
  value that the calling scraper (`ItemScraper` or `CreatureScraper`) can
  detect and use to skip that page cleanly.
- In `ItemScraper.java` and `CreatureScraper.java`, wherever the result of the
  `WikiApiClient` call is consumed, add a null/empty check: if the response is
  empty, log a second `WARN` at the scraper level (e.g. `"Skipping page
  'Dragon' due to HTTP error — will be missing from this run"`) and continue
  to the next page without throwing.
- All other non-200 status codes (e.g. 500, 404) retain the existing behaviour
  of throwing `ScraperException` and aborting the run.

**File to modify:** `src/.../scraper/ItemScraper.java`

- Add the empty/skip check described above wherever the `WikiApiClient` fetch
  result is consumed.

---

## Verification Criteria
- [ ] After a full scrape, `SELECT physicalDmgMod, fireDmgMod, iceDmgMod
      FROM creatures WHERE name = 'Dragon'` returns `100`, `0`, `110`
      respectively — no NULL resistance columns on any creature row.
- [ ] After a full scrape, `SELECT COUNT(*) FROM creature_loot` is greater
      than zero.
- [ ] The log shows no `"Unknown loot item"` warnings for `Gold Coin`,
      `Dragon Ham`, `Steel Shield`, or other items confirmed to exist in the
      `items` table.
- [ ] Items with no amount field (e.g. `Steel Shield`) are linked correctly
      in `creature_loot` — the rarity string is not mistaken for the item name.
- [ ] No `NumberFormatException` is thrown during resistance parsing for any
      creature page that contains `%`-suffixed values.
- [ ] When the API returns 403 or 429 for a page, a WARN line appears in the
      log containing the status code, the page name, and (for 429) the hint
      about `tlim.scraper.delay-ms`.
- [ ] The scrape run continues and completes after a 403 or 429 — it does not
      abort.
- [ ] A second scrape run after a partial first run produces no duplicate rows
      in `creatures` or `creature_loot` — the upsert behaviour is preserved.
- [ ] Any non-200 status code other than 403 and 429 still throws
      `ScraperException` and aborts the run.
