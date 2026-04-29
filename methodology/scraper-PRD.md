# PRD — TibiaWiki Scraper Feature

## What We Are Building

A backend scraper module that automatically fetches data from the TibiaWiki
public API, parses it, and populates a PostgreSQL database with all items and
creatures from the game Tibia. The scraper is triggered manually by an admin
via a secured HTTP endpoint, runs in the background without blocking the server,
and prevents duplicate concurrent executions. The scraped data feeds both an
in-app wiki and an inventory optimization engine.

## Who It Is For

A single administrator (the developer) who triggers the scrape manually after
game updates. End users of the application consume the scraped data indirectly
through the wiki and inventory features — they never interact with the scraper
directly.

## Confirmed Tech Stack

| Technology | Role in this feature |
|---|---|
| Java 17 | Main programming language |
| Spring Boot 3.5.0 | Application framework — handles HTTP, dependency injection, configuration |
| Spring Data JPA | Translates Java objects into database rows and queries |
| PostgreSQL | Persistent database where scraped data is stored |
| Flyway | Manages database table creation and migrations in a controlled, versioned way |
| Jsoup 1.17.2 | HTML parsing library — present in pom.xml but currently unused in the scraper |
| Jackson (bundled) | Parses JSON responses from the TibiaWiki API |
| Java HttpClient (built-in) | Makes HTTP requests to the TibiaWiki API |
| JJWT 0.12.5 | Secures the admin endpoint with JWT authentication |
| SpringDoc OpenAPI 2.8.0 | Generates interactive API documentation (Swagger UI) |
| Maven | Builds the project and manages dependencies |

## Existing Affected Files

| File | Reason |
|---|---|
| `src/.../scraper/WikiApiClient.java` | Makes all HTTP calls to the TibiaWiki MediaWiki API |
| `src/.../scraper/ItemScraper.java` | Fetches and parses all item pages; saves items and NPC buyers |
| `src/.../scraper/CreatureScraper.java` | Fetches and parses all creature pages; saves creatures and loot links — **contains both known bugs** |
| `src/.../scraper/ScraperService.java` | Orchestrates item scrape then creature scrape; returns combined result |
| `src/.../scraper/ScraperProperties.java` | Holds configurable delay between API requests (tlim.scraper.delay-ms) |
| `src/.../scraper/ScraperResult.java` | Data record returned at the end of a full scrape run |
| `src/.../scraper/ScraperException.java` | Custom exception for HTTP and parsing failures |
| `src/.../scraper/dto/ScrapeStatusResponse.java` | API response shape for scrape status endpoint |
| `src/.../admin/AdminController.java` | HTTP endpoints to trigger and poll the scrape; holds concurrency lock |
| `src/.../creature/Creature.java` | JPA entity mapped to the `creatures` table |
| `src/.../creature/CreatureLoot.java` | JPA entity mapped to the `creature_loot` join table |
| `db/migration/V6__create_creatures.sql` | Flyway migration that creates the `creatures` table |

## New Dependencies Needed

None. All required libraries are already declared in `pom.xml`.

---

## How This Is Typically Built

### Overall pattern: Category-walk → Page-fetch → Parse → Upsert

The TibiaWiki exposes a public MediaWiki API at `https://tibia.fandom.com/api.php`.
The standard approach for bulk scraping a MediaWiki wiki is:

1. Query the `categorymembers` endpoint to list all page titles in a category
2. For each title, call the `parse` endpoint with `prop=wikitext` to get the raw
   wiki markup of that page
3. Parse the markup to extract infobox fields (structured data between `{{` and `}}`)
4. Save or update the record in the database (upsert pattern)

This is exactly what the current code does. The implementation is architecturally
sound. The problems are in the parsing details, not the structure.

### Wikitext infobox format (reference)

A typical TibiaWiki creature page contains a block like:

```
{{Infobox Creature
| name        = Dragon
| hp          = 1000
| exp         = 700
| physical    = 100
| fire        = 0
| ice         = 110
...
}}
```

Resistance values are percentages. 100 means normal damage taken. Values below
100 mean the creature resists that element. Values above 100 mean it takes extra
damage. The wiki sometimes writes these as `100%` (with the percent sign) or just
`100` (without it). The parser must handle both.

### Loot block format (reference)

```
{{Loot2
| {{Loot Item|0-4|Gold Coin|always}}
| {{Loot Item|0-1|Dragon Ham|common}}
}}
```

The item name inside `{{Loot Item|...}}` must match exactly (case-sensitive) the
`name` field stored in the `items` table for the foreign key link to resolve.

---

## Known Bugs and Root Causes

### Bug 1 — Elemental resistances not saved to the database

**Symptom:** Creature rows are created but all resistance columns are NULL.

**Root cause:** The `parseResistance()` method in `CreatureScraper.java` tries to
parse the value of fields like `physical`, `fire`, `ice`, etc. However, on
TibiaWiki the actual infobox field names for resistances follow the pattern
`res physical`, `res fire`, `res ice` — with the prefix `res ` — not the short
form without the prefix. The `parseFields()` method lowercases all keys, so the
code is looking for `"physical"` in the map but the actual key stored is
`"res physical"`. The values are present in the wikitext but looked up under the
wrong key, so they always fall through to the `null` default.

**Fix area:** The field key lookup in `scrapeCreature()` inside `CreatureScraper.java`.
Either the keys used in `fields.get(...)` calls must be updated to match the actual
wiki field names, or the parser must strip the `res ` prefix when building the map.
This must be verified against a real TibiaWiki page before fixing — the exact field
names should be confirmed by fetching one creature page raw from the API.

### Bug 2 — Creature loot not linked to items ("Unknown loot item" warnings)

**Symptom:** The log shows `"Unknown loot item 'X' for creature 'Y' — skipping"`
for most or all loot entries. The `creature_loot` table stays empty.

**Root cause — likely cause A (name mismatch):** The `{{Loot Item}}` parser in
`parseLootEntries()` splits on `|` and takes `parts[2]` as the item name. However,
the `{{Loot Item}}` template on TibiaWiki does not have a fixed positional format.
The real format is `{{Loot Item|amount=0-4|name=Gold Coin|type=always}}` using
named parameters, not positional ones. The current code reads positional slots and
extracts the wrong value as the item name, so the lookup `itemRepository.findByName()`
never finds a match.

**Root cause — likely cause B (ordering dependency):** Even if the name extraction
were correct, if the item scrape did not complete successfully before the creature
scrape runs, the items won't exist in the database yet and all loot lookups will
fail. `ScraperService` does run items first sequentially, which is correct — but
if the item scrape partially failed, loot links for those missing items will still
produce warnings. This is expected and acceptable behavior, not a bug.

**Fix area:** The `parseLootEntries()` method in `CreatureScraper.java`. The parsing
logic must be updated to handle the named-parameter format of `{{Loot Item}}`. Again,
this must be verified against a real creature page from the API before implementing.

---

## Risks and Edge Cases

- **TibiaWiki format inconsistency:** Different creature pages may use slightly
  different infobox field names or template variants (e.g., `{{Loot}}` vs `{{Loot2}}`).
  The code already handles both loot variants, but resistance field names must be
  confirmed per the actual wiki source.

- **Rate limiting:** The scraper respects a configurable delay (`tlim.scraper.delay-ms`)
  between requests. If TibiaWiki starts returning HTTP 429 (too many requests), the
  current code will throw a `ScraperException` and abort. There is no retry logic.
  This is an acceptable limitation for a manually-triggered portfolio scraper.

- **Loot items not yet in the database:** If an item exists in a creature's loot
  table on the wiki but was not scraped in the item pass (e.g., it's in an uncovered
  category), the loot link is silently skipped with a warning. This is a data coverage
  issue, not a code bug.

- **Concurrency protection:** `AdminController` uses an `AtomicBoolean` flag to
  prevent two simultaneous scrape runs. This works correctly for a single server
  instance. If the application were ever deployed across multiple servers, this
  in-memory flag would not work — but for a single-instance portfolio project this
  is a non-issue.

- **Memory during scrape:** All page titles are collected into a `LinkedHashSet`
  in memory before processing begins. For thousands of pages this is fine, but
  worth noting as a scaling consideration.

- **No persistence of scrape status:** The `lastStatus` field in `AdminController`
  is stored in memory. If the server restarts mid-scrape or after completion, the
  status is lost and resets to IDLE.

---

## Open Questions

1. **Resistance field names:** What are the exact infobox field names used on
   TibiaWiki for elemental resistances? This must be confirmed by fetching one
   real creature page (e.g., Dragon) from the API with `prop=wikitext` before
   the fix can be written. The answer changes exactly which keys to look up in
   `scrapeCreature()`.

2. **Loot Item template format:** Is `{{Loot Item}}` positional or named-parameter
   on the current version of TibiaWiki? This must be confirmed the same way —
   fetch one creature page with known loot and inspect the raw wikitext.

3. **Item scrape coverage:** Are all item categories reachable under `Category:Items`
   subcategories, or are some items (particularly quest-only or seasonal items)
   listed under different top-level categories? If loot warnings persist after
   fixing the name parser, this is the next thing to investigate.
