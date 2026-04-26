# TLIM — Tibia Loot & Inventory Manager
### Stage 2: Java / Spring Boot / PostgreSQL

> A complete rewrite of [TLIM Stage 1](https://github.com/calgadev/tlim-python2) — originally built in Python/FastAPI/SQLite — now rebuilt as a production-grade REST API.

---

## What is TLIM?

TLIM is a personal portfolio project built to solve real problems faced by players of the MMORPG [Tibia](https://www.tibia.com):

- **What should I do with my loot after a hunt?** Keep it, sell to an NPC, or list on the market?
- **Where should I grind?** Which hunting spots actually generate the most value over time?

Stage 1 proved the concept with a working Python/FastAPI MVP. Stage 2 rebuilds the foundation to support a richer feature set: a mini wiki populated by a TibiaWiki scraper, JWT authentication, NPC seller filtering, item goal tracking, and a fully documented REST API via Swagger UI.

A React frontend is planned for Stage 3.

---

## How this project is being built

TLIM Stage 2 is developed using a structured AI-assisted development methodology — a pipeline that takes a project from idea to deploy using large language models at each stage of the process.

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

## Tech stack

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

## Prerequisites

- Java 17+
- Maven 3.x
- PostgreSQL 15+

---

## Environment variables

The application reads all sensitive configuration from environment variables. No credentials are stored in source or config files.

| Variable | Description |
|---|---|
| `DB_URL` | JDBC connection URL (e.g. `jdbc:postgresql://localhost:5432/tlim`) |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key used to sign JWT tokens |

Copy `.env.example` to `.env` and fill in the values before running locally.

---

## Running locally

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

## API reference

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
| Admin | `/api/admin` |

---

## Creating the first admin user

There is no registration endpoint that creates admin users. The first admin must be inserted directly into the database with a BCrypt-hashed password:

```sql
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '<bcrypt_hash>', 'ADMIN');
```

Generate the BCrypt hash using any standard tool before inserting. The `POST /api/admin/scrape` endpoint (which triggers the TibiaWiki crawler) is restricted to admin-role tokens only.

---

## Domain model

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
| `HuntSession` | Hunt record linked to a character, with status and loot entries |
| `HuntSessionItem` | Individual loot entry within a hunt session |

---

## TibiaWiki scraper

The scraper performs a three-level crawl of [tibia.fandom.com](https://tibia.fandom.com) using Jsoup, populating items and creatures via upsert (no duplicates on re-run). A 700ms delay between requests is applied to avoid rate limiting.

Triggered via `POST /api/admin/scrape` (ADMIN role required). Scrape status is available at `GET /api/admin/scrape/status`.

---

## What is NOT in scope for Stage 2

- React frontend (Stage 3)
- Market price tracking — NPC buyer prices only, no player market data
- Hunt Analyser text/JSON import — manual hunt session entry via API only
- Email, OAuth, or any auth mechanism beyond username + password JWT
- Role-based access control beyond USER / ADMIN
- Tibia.com API integration
- Push notifications or webhooks
- Soft deletes or audit logging

---

## Project roadmap

### Stage 1 — Python / FastAPI / SQLite
- [x] 10 SQLAlchemy models
- [x] Hunt session import (text and JSON formats from Tibia Hunt Analyser)
- [x] Sale decision engine (Keep / Sell to NPC / Sell on market / No price available)
- [x] Inventory management
- [x] Market price management
- [x] Seed script

### Stage 2 — Java / Spring Boot / PostgreSQL *(current)*
- [x] Project initialization and base configuration
- [x] Server entity and database verification
- [x] Item entity
- [ ] Full domain model and Flyway migrations (V1–V11)
- [ ] JWT authentication
- [ ] REST API endpoints (all groups)
- [ ] TibiaWiki scraper (items and creatures)
- [ ] Swagger UI documentation
- [ ] Deploy

### Stage 3 — React frontend *(planned)*

---

## Author

**Guilherme Calgaro**
Systems Analyst | AI-assisted development methodologies
[LinkedIn](https://www.linkedin.com/in/guilherme-de-oliveira-calgaro/) · [GitHub](https://github.com/calgadev)
