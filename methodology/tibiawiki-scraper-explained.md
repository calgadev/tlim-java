# How the TibiaWiki Scraper Works

## Context

One of the features of this project is an automatic data pipeline that populates
a PostgreSQL database with every item and creature from the game Tibia. The data
feeds an in-app wiki and an inventory optimization engine. Rather than maintaining
this data by hand, I built a scraper that fetches it directly from TibiaWiki —
the community-maintained encyclopedia for the game.

---

## The Data Source: A Public MediaWiki API

TibiaWiki is hosted on Fandom, which runs on MediaWiki — the same engine that
powers Wikipedia. Every MediaWiki installation exposes a free, public, read-only
HTTP API at `/api.php`. No authentication, no API key, no special access required.

For TibiaWiki, the base URL is:

```
https://tibia.fandom.com/api.php
```

This is not web scraping in the traditional sense. There is no parsing of HTML,
no fighting against CSS selectors, and no risk of breaking when the site redesigns
its layout. The API returns structured data — either JSON or raw wikitext — which
is far more stable and reliable.

---

## The Four-Step Pipeline

### Step 1 — Discover all page titles in a category

The MediaWiki API exposes a `categorymembers` endpoint that lists every page
belonging to a given category. The scraper calls this endpoint for the relevant
categories (e.g. `Category:Creatures`, `Category:Items`) and collects all page
titles into memory.

```
GET /api.php?action=query&list=categorymembers&cmtitle=Category:Creatures&cmlimit=500
```

The API paginates results, so the scraper follows continuation tokens until it
has the complete list. The result is a set of titles like `["Dragon", "Demon",
"Rat", ...]` — one per wiki page.

### Step 2 — Fetch the raw wikitext for each page

For each title, the scraper calls the `parse` action with `prop=wikitext`:

```
GET /api.php?action=parse&page=Dragon&format=json&prop=wikitext
```

This returns the raw wikitext that wiki editors write — the structured markup
behind the rendered page. For creature pages, this always contains an
`{{Infobox Creature|...}}` block with all the data fields:

```
{{Infobox Creature|...
| name           = Dragon
| hp             = 1000
| exp            = 700
| physicalDmgMod = 100%
| fireDmgMod     = 0%
| iceDmgMod      = 110%
...
| loot           = {{Loot Table
 |{{Loot Item|1-105|Gold Coin|common}}
 |{{Loot Item|Steel Shield|uncommon}}
}}
```

A configurable delay is applied between requests (`tlim.scraper.delay-ms`) to
avoid overwhelming the API.

### Step 3 — Parse the infobox into structured data

A custom parser reads the raw wikitext and extracts the key-value pairs from
inside the infobox block into a `Map<String, String>`. From there, each field
is mapped to the corresponding Java entity attribute — with type coercion where
needed (e.g. stripping trailing `%` from resistance values before parsing them
as integers).

Loot entries require slightly more care: the `{{Loot Item}}` template is
positional, and the first segment may be either an amount range (`1-105`) or
directly the item name when no amount is specified. The parser detects which
variant it is reading and extracts the item name accordingly, then links it to
the matching record in the `items` table via a foreign key.

### Step 4 — Upsert into PostgreSQL

The parsed data is saved to the database using an upsert pattern via Spring
Data JPA. If a creature or item already exists (matched by name), its record
is updated. If it does not exist, a new row is inserted. This means the scraper
can be re-run safely after game updates without creating duplicates.

---

## The Debugging Challenge

The two hardest bugs to track down were both silent: no exceptions were thrown,
rows were created, but columns were left NULL and join tables stayed empty.

**Bug 1 — Resistances always NULL.** The parser was looking up fields like
`"physical"` and `"fire"` in the map, but the actual keys from the wikitext are
camelCase: `physicalDmgMod`, `fireDmgMod`, `iceDmgMod`, etc. The values were
present but looked up under the wrong keys, so they always fell through to the
`null` default. The fix required fetching a real creature page from the API,
reading the raw wikitext, and discovering the actual field names — something
no amount of reading the code alone would have revealed.

**Bug 2 — Loot links never resolved.** The loot parser assumed the item name
was always at position `parts[2]` after splitting on `|`. But for items with no
amount (e.g. `{{Loot Item|Steel Shield|uncommon}}`), the name is at `parts[1]`.
The parser was reading the rarity string as the item name, so
`itemRepository.findByName("uncommon")` never matched anything. Again, the fix
only became clear after inspecting real wikitext from the live API.

Both bugs illustrate the same lesson: when integrating with an external data
source, assumptions about format must be verified against real data — not inferred
from documentation or guesswork.

---

## Architecture at a Glance

| Component | Role |
|---|---|
| `WikiApiClient` | Makes all HTTP calls to the MediaWiki API using Java's built-in `HttpClient` |
| `ItemScraper` | Fetches and parses all item pages; saves items and NPC buyer links |
| `CreatureScraper` | Fetches and parses all creature pages; saves creatures and loot links |
| `ScraperService` | Orchestrates the full run: items first, then creatures |
| `AdminController` | Exposes a JWT-secured HTTP endpoint to trigger the scrape; uses an `AtomicBoolean` to prevent concurrent runs |
| `ScraperProperties` | Holds the configurable request delay (`tlim.scraper.delay-ms`) |

The scrape is triggered manually by the administrator after game updates and runs
entirely in the background without blocking the HTTP server.
