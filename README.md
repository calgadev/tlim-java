# TLIM — Tibia Loot & Inventory Manager
### Stage 2: Java / Spring Boot / PostgreSQL

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Complete-success?style=for-the-badge)

> A complete rewrite of [TLIM Stage 1](https://github.com/calgadev/tlim-python2) — originally built in Python/FastAPI/SQLite — now rebuilt as a production-grade REST API. A React frontend is planned for Stage 3.

---

## What is TLIM?

TLIM is a personal portfolio project built to solve real problems faced by players of the MMORPG [Tibia](https://www.tibia.com):

- **What should I do with my loot after a hunt?** Keep it, sell to an NPC, or list on the market?
- **Where should I grind?** Which hunting spots actually generate the most value over time?

Stage 1 proved the concept with a working Python/FastAPI MVP. Stage 2 rebuilds the foundation to support a richer feature set: a mini wiki populated by a TibiaWiki scraper, JWT authentication, NPC seller filtering, item goal tracking, and a fully documented REST API via Swagger UI.

---

## Project Roadmap

| Stage | Stack | Repository | Status |
|---|---|---|---|
| Stage 1 — Backend + Server-side UI | Python 3.12 · FastAPI · SQLAlchemy 2.0 · SQLite · Jinja2 | tlim-python2 | ✅ Complete |
| Stage 2 — Backend rewrite | Java 17 · Spring Boot · Spring Data JPA · PostgreSQL · Maven | tlim-java | ✅ Complete |
| Stage 3 — Modern frontend | React · JavaScript ES6+ · React Router · Axios | tlim-frontend (coming soon) | 🚧 In Progress |

The project is intentionally developed in three stages with different stacks to demonstrate that the same problem can be solved across different languages and technologies — showing transferable knowledge rather than familiarity with a single tool.

---

## How this project was built

Stage 2 was developed using a structured AI-assisted development methodology — a pipeline that takes a project from idea to deploy using large language models at each stage of the process.

| Stage | Purpose |
|---|---|
| **Research** | Understand the problem domain, constraints, and prior art before writing any spec |
| **Specification** | Produce a detailed, unambiguous spec that drives all subsequent development |
| **Setup** | Initialize the project environment, dependencies, and base configuration |
| **Issue breakdown** | Decompose the spec into discrete, implementation-ready Kanban cards |
| **Code generation** | Write production code following the spec and security requirements |
| **Code review** | Audit generated code for security issues, sensitive data exposure, spec conformance, and code quality — before any commit |
| **Debug** | Isolate and fix errors surfaced during development or testing |
| **Deploy** | Package and deploy the application to the target environment |

Each stage uses purpose-built prompts and skills designed to produce consistent, auditable outputs. The code review stage acts as a quality gate: no code is committed without passing security and conformance checks.

This methodology is the foundation of a future SaaS product aimed at enabling non-developers to go from idea to deployed microSaaS using AI. TLIM Stage 2 is its first real-world validation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Security | Spring Security + JWT (stateless, Bearer token) |
| Scraper | Jsoup (three-level TibiaWiki crawl) |
| API docs | springdoc-openapi / Swagger UI |
| Build | Maven |

---

## Architecture

```
src/
├── main/
│   ├── java/com/tlim/
│   │   ├── TlimApplication.java        ← Spring Boot entry point
│   │   ├── admin/                      ← Scraper trigger and status endpoints (ADMIN role)
│   │   ├── auth/                       ← JWT authentication, security filter chain, UserDetails
│   │   │   └── dto/                    ← Register, login, and token response DTOs
│   │   ├── character/                  ← Character CRUD, vocation enum, user-scoped queries
│   │   │   └── dto/
│   │   ├── config/                     ← Security config, OpenAPI/Swagger config, global exception handler
│   │   ├── creature/                   ← Creature CRUD with loot eager-loading
│   │   │   └── dto/
│   │   ├── hunt/                       ← Hunt session CRUD, import service, text and JSON parsers
│   │   │   ├── dto/
│   │   │   └── parser/                 ← TextHuntParser, JsonHuntParser, ParsedHunt dataclasses
│   │   ├── inventory/                  ← Inventory management, sale decision engine
│   │   │   └── dto/
│   │   ├── item/                       ← Item CRUD, NPC buyers, server market prices
│   │   │   └── dto/
│   │   ├── scraper/                    ← Three-level TibiaWiki MediaWiki API crawl
│   │   │   └── dto/
│   │   ├── server/                     ← Server CRUD, PVP type enum
│   │   │   └── dto/
│   │   └── user/                       ← User entity and repository
│   └── resources/
│       ├── application.properties      ← All credentials read from environment variables
│       ├── application-prod.properties
│       └── db/migration/               ← Flyway versioned migrations (V1–V12)
└── test/
    └── java/com/tlim/
        └── TlimApplicationTests.java
```

---

## Key Technical Decisions

**Stateless JWT over session-based auth** — Spring Security is configured as `STATELESS`, meaning no `HttpSession` is ever created. The Bearer token carries identity on every request, which fits a REST API that will serve a React frontend in Stage 3.

**`marketPrice` as nullable `Integer`, not primitive `int`** — `NULL` means "price not yet registered", which is semantically distinct from `0` (a known worthless price). Using a primitive would collapse both states into zero.

**Upsert over delete-and-reinsert on scraper re-runs** — Items and creatures are updated in place via `findByName()` + save. NPC buyers are the exception: they are deleted and reinserted per item, since the buyer list from the wiki is always a complete replacement. This keeps re-runs idempotent with no duplicate rows.

**700ms delay between scraper requests** — Applied in `WikiApiClient` before every HTTP call via `Thread.sleep()`. Configurable via `tlim.scraper.delay-ms` in `application.properties`. Prevents rate limiting from Fandom's API without requiring a retry mechanism.

**Feature-based packages over layer-based** — Each domain (`auth`, `character`, `hunt`, etc.) owns its own controller, service, repository, and DTOs. This keeps all code related to a feature co-located, making it easier to reason about and eventually extract into a microservice.

**`balance` not stored** — Calculated as `lootTotal - supplies`. Both values are immutable after import, so storing the difference would be redundant. It is computed at query time in the response.

---

## Prerequisites

- Java 17+
- Maven 3.x
- PostgreSQL 15+

---

## Environment Variables

The application reads all sensitive configuration from environment variables. No credentials are stored in source or config files.

| Variable | Description |
|---|---|
| `DB_URL` | JDBC connection URL (e.g. `jdbc:postgresql://localhost:5432/tlim`) |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key used to sign JWT tokens |

Copy `.env.example` to `.env` and fill in the values before running locally.

---

## Running Locally

```bash
# 1. Clone the repository
git clone https://github.com/calgadev/tlim-java.git
cd tlim-java

# 2. Copy and fill in environment variables
cp .env.example .env

# 3. Build and run
./mvnw spring-boot:run
```

Flyway will run all migrations automatically on first start. The API will be available at `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## API Reference

All endpoints are documented and testable via Swagger UI at `/swagger-ui.html`.

Protected endpoints require a Bearer JWT token. Use `POST /api/auth/register` to create a user and `POST /api/auth/login` to obtain a token.

| Group | Base path |
|---|---|
| Auth | `/api/auth` |
| Servers | `/api/servers` |
| Characters | `/api/characters` |
| Items | `/api/items` |
| Creatures | `/api/creatures` |
| Inventory | `/api/inventory` |
| Hunt Sessions | `/api/hunt-sessions` |
| Hunt Analyser Import | `POST /api/hunt-sessions/import/text`, `POST /api/hunt-sessions/import/json` |
| Server Item Prices | `/api/servers/{serverId}/item-prices` |
| Admin | `/api/admin` |

---

## Search and Filter Query Params

| Endpoint | Parameter | Behaviour |
|---|---|---|
| `GET /api/items` | `?name=rotworm` | Returns items whose name contains the value (case-insensitive). Absent or blank falls through to all items. |
| `GET /api/items` | `?category=weapon` | Returns items in that category. Takes precedence over `?name=` when both are supplied. |
| `GET /api/hunt-sessions/characters/{id}` | `?location=Drefia` | Returns sessions whose location contains the value (case-insensitive). Absent or blank returns all sessions for the character. |

---

## Hunt Analyser Import

Two endpoints accept hunt session data exported from the in-game Hunt Analyser:

| Endpoint | Format |
|---|---|
| `POST /api/hunt-sessions/import/text` | Raw text copied from the Hunt Analyser |
| `POST /api/hunt-sessions/import/json` | JSON exported from the Hunt Analyser |

**Required fields**

| Field | Type | Description |
|---|---|---|
| `characterId` | Long | ID of the character who ran the hunt |
| `rawData` | String | The full text or JSON content from the Hunt Analyser |

**Optional fields**

| Field | Type | Description |
|---|---|---|
| `name` | String | Session label. If omitted or blank, auto-generated as `Hunt dd/MM HH:mm` from the session start time |
| `isParty` | Boolean | `true` if the hunt was run in a party. Defaults to `false` if omitted |
| `notes` | String | Free-text notes for this session |
| `charLevel` | Integer | Character level at the time of the hunt |
| `allyEkLevel` | Integer | Level of the party's Elite Knight |
| `allyMsLevel` | Integer | Level of the party's Master Sorcerer |
| `allyEdLevel` | Integer | Level of the party's Elder Druid |
| `allyRpLevel` | Integer | Level of the party's Royal Paladin |
| `allyEmLevel` | Integer | Level of the party's Exalted Monk |
| `location` | String | Hunting spot description (free text) |

**Response — skipped names**

If the raw data contains item or creature names not found in the database, the import does not fail. Instead, the response includes:

| Field | Type | Description |
|---|---|---|
| `skippedItems` | `List<String>` | Item names from the parsed data that had no match in the Items table. Empty list when all matched. |
| `skippedMonsters` | `List<String>` | Creature names from the parsed data that had no match in the Creatures table. Empty list when all matched. |

A 201 response with non-empty skip lists means the session was saved successfully — only the unrecognised entries were omitted. Run the TibiaWiki scraper (`POST /api/admin/scrape`) if items or creatures are missing.

---

## What is NOT in scope for Stage 2

- React frontend (Stage 3)
- Email, OAuth, or any auth mechanism beyond username + password JWT
- Role-based access control beyond USER / ADMIN
- Tibia.com API integration
- Push notifications or webhooks
- Soft deletes or audit logging

---

## Project Roadmap

### Stage 1 — Python / FastAPI / SQLite
- [x] Project structure and virtual environment
- [x] Database configuration (SQLAlchemy + SQLite)
- [x] Base FastAPI app running with Uvicorn
- [x] 10 SQLAlchemy models (User, Server, Character, Item, Creature, ServerItemPrice, Inventory, HuntSession, HuntSessionItem, HuntSessionMonster)
- [x] Seed script (30 servers, 20 items, 20 creatures)
- [x] User and character management
- [x] Hunt session import — text and JSON formats
- [x] Hunt history and detail views
- [x] Inventory management with stock goals
- [x] Sale decision engine (Keep / Sell to NPC / Sell on market / No price available)
- [x] Market price management per server

### Stage 2 — Java / Spring Boot / PostgreSQL *(current)*
- [x] Project initialization and base configuration
- [x] Full domain model and Flyway migrations (V1–V12)
- [x] JWT authentication (register, login, stateless Bearer token, security filter chain)
- [x] Swagger UI with JWT bearer auth scheme
- [x] Server CRUD API (`/api/servers`)
- [x] Character CRUD API with user-scoped queries (`/api/characters`)
- [x] Item CRUD API with category filter (`/api/items`)
- [x] Creature CRUD API with loot eager-loading (`/api/creatures`)
- [x] Inventory API (`/api/inventory`)
- [x] Hunt Sessions API (`/api/hunt-sessions`)
- [x] Hunt Analyser import — text and JSON formats (`/api/hunt-sessions/import`)
- [x] Server Item Prices API — per-server market price upsert and list (`/api/servers/{serverId}/item-prices`)
- [x] Sale Decision Engine — per-character sell recommendations (`GET /api/inventory/characters/{id}/decisions`)
- [x] Admin API + TibiaWiki scraper (`/api/admin`)
- [x] Search and filter query params — `?name=` on items, `?location=` on hunt sessions
- [ ] Deploy

### Stage 3 — React frontend *(planned)*

---

## Creating the First Admin User

There is no registration endpoint that creates admin users. The first admin must be inserted directly into the database with a BCrypt-hashed password:

```sql
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '<bcrypt_hash>', 'ADMIN');
```

Generate the BCrypt hash using any standard tool before inserting. The `POST /api/admin/scrape` endpoint (which triggers the TibiaWiki crawler) is restricted to admin-role tokens only.

---

## Domain Model

The application is structured around feature-based packages (`com.tlim.<domain>`). Core entities:

| Entity | Description |
|---|---|
| `User` | Authenticated user account |
| `Server` | Tibia game server (e.g. Antica, Rubera) |
| `Character` | Player character linked to a user and server |
| `Item` | Game item populated by the TibiaWiki scraper |
| `NpcBuyer` | NPC that buys a given item, with location and price |
| `Creature` | Game creature with stats and resistances, populated by scraper |
| `CreatureLoot` | Loot table entry linking creature to item with rarity and amount range |
| `Inventory` | Per-character item tracking with current and target quantities |
| `HuntSession` | Closed hunt record linked to a character, with analytics and loot entries |
| `HuntSessionItem` | Individual loot entry within a hunt session |
| `HuntSessionMonster` | Individual kill entry within a hunt session |

---

## TibiaWiki Scraper

The scraper performs a three-level crawl of [tibia.fandom.com](https://tibia.fandom.com) using the MediaWiki JSON API, populating items and creatures via upsert (no duplicates on re-run). A 700ms delay between requests is applied to avoid rate limiting.

Triggered via `POST /api/admin/scrape` (ADMIN role required). Scrape status is available at `GET /api/admin/scrape/status`.

---

## Deployment (Linux / systemd)

A systemd service unit (`tlim.service`) is included for running the application as a managed background service on Linux.

**1. Create the environment file**

Copy your environment variables to `/home/calga/tlim-java/.env.service`. This file is excluded from version control and is separate from your local `.env`.

```
DB_URL=jdbc:postgresql://localhost:5432/tlim
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
```

**2. Install and enable the service**

```bash
sudo cp tlim.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable tlim
sudo systemctl start tlim
```

**3. Check status**

```bash
sudo systemctl status tlim
journalctl -u tlim -f
```

The service restarts automatically on failure with a 10-second delay.

---

## Sobre o projeto

O TLIM nasceu de duas dores reais de um jogador de Tibia: saber o que fazer com o loot após uma hunt, e ter histórico estruturado de sessões para comparar onde vale mais a pena caçar.

Este projeto é a segunda etapa de um portfólio desenvolvido em três stacks diferentes — Python/FastAPI/SQLite, Java/Spring Boot/PostgreSQL e React. O Stage 2 reescreve a base do Stage 1 como uma API REST de nível produção, adicionando autenticação JWT, um scraper do TibiaWiki e documentação completa via Swagger UI. A ideia central é mostrar que o mesmo problema pode ser resolvido com ferramentas diferentes, evidenciando raciocínio transferível em vez de familiaridade com uma única tecnologia.

Faz parte de uma transição de carreira de Analista de Sistemas para Desenvolvedor.

---

## Author

**Guilherme Calgaro**
Systems Analyst | AI-assisted development methodologies  
[LinkedIn](https://www.linkedin.com/in/guilherme-de-oliveira-calgaro/) · [GitHub](https://github.com/calgadev)
