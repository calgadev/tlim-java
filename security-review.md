# Security Review — TLIM API — Full Codebase
> Reviewed: 2026-04-29 | Branch: main | Scope: all source files

---

## Summary

| Severity | Count |
|----------|-------|
| 🔴 Critical (blocks public deploy) | 0 |
| 🟡 Important (should fix before deploy) | 0 |
| 🟢 Minor (optional) | 2 |
| ✅ Passed | 20 |

**All critical and important issues from the previous review have been resolved. The codebase is safe to make public and deploy.**

---

## ✅ What Passed

- **No hardcoded secrets in source code** — `application.properties` uses `${ENV_VAR}` for all sensitive values
- **`.env` is in `.gitignore`** and was never committed to git history
- **`.env.example` exists at root** with placeholder values and a warning against weak production passwords
- **JWT secret read from environment variable** — `JwtService.java:24`
- **Passwords BCrypt-hashed** — never stored in plaintext — `AuthService.java:39`
- **Password minimum length enforced** — `RegisterRequest.java` requires `@Size(min = 8, max = 100)`
- **Generic authentication error messages** — `GlobalExceptionHandler.java:58-61` never reveals whether username or password was wrong
- **Stack traces never exposed** — `GlobalExceptionHandler.java:70-73` logs internally, returns safe 500 message
- **CSRF disabled with correct reasoning** — stateless JWT API, comment explains why — `SecurityConfig.java:30`
- **ADMIN role enforced at URL level** — `SecurityConfig.java:37` guards `/api/admin/**`
- **Swagger UI disabled in production** — `application-prod.properties:2-3` disables both `/swagger-ui` and `/api-docs`
- **SSRF prevented in WikiApiClient** — BASE_URL hardcoded, not user-configurable — `WikiApiClient.java:25`
- **URL parameters properly encoded** — `WikiApiClient.java:40-43`
- **User-supplied names truncated before logging** — `HuntImportService.java:168` prevents log flooding
- **JWT filter handles malformed tokens gracefully** — `JwtAuthFilter.java:42-46`
- **BOLA: character update/delete** — `CharacterService.java:63-65, 84-86` checks ownership before mutating; returns 404 to prevent data leakage
- **BOLA: inventory upsert/list** — `InventoryService.java:32-34, 55-57` checks character ownership before any data access
- **BOLA: inventory entry read/delete** — `InventoryService.java:65-67, 76-78` checks ownership; returns 404 to prevent data leakage
- **BOLA: hunt session read** — `HuntSessionService.java:54-56` checks ownership; returns 404 to prevent data leakage
- **BOLA: hunt import** — `HuntImportService.java:60-62` checks character ownership before persisting session data

---

## 🟢 Minor (optional — do not block deploy)

### 1. Inconsistent ownership-failure response code across services

Some services return `404 EntityNotFoundException` when the resource belongs to another user (to prevent data leakage), while others return `403 AccessDeniedException`. Both are valid security choices, but the inconsistency makes the API harder to reason about.

**Mixed behaviour within the same resource type:**
- `HuntSessionService.getSessionById` → 404 (prevents leaking existence)
- `HuntSessionService.deleteSession` → 403 (reveals the session exists)

**Pattern breakdown:**
- Character-scoped list/write operations (`upsertInventory`, `getInventoryByCharacter`, `getSessionsByCharacter`, hunt imports) → 403
- Direct resource ID operations → mixed (some 404, some 403)

**Recommendation:** Pick one pattern and apply it consistently per resource type. The 404 pattern is slightly stronger (prevents enumeration), but either is acceptable.

---

### 2. jsoup 1.17.2 is not the latest patch
**File:** `pom.xml:94`

jsoup 1.18.x is available. The current version (1.17.2) has no known critical CVEs in this usage context, but updating is low-risk and keeps dependencies current.

**Fix:** Update to `1.18.3` (or latest) in `pom.xml`.

---

## Resolved Issues (from previous review)

The following 8 critical BOLA vulnerabilities and 4 important issues were identified in the previous review and are now confirmed fixed:

| # | Issue | Status |
|---|-------|--------|
| 1 | BOLA: `PUT /api/characters/{id}` — no ownership check | ✅ Fixed in `CharacterService.java:63-65` |
| 2 | BOLA: `DELETE /api/characters/{id}` — no ownership check | ✅ Fixed in `CharacterService.java:84-86` |
| 3 | BOLA: `POST /api/inventory/characters/{characterId}` — no ownership check | ✅ Fixed in `InventoryService.java:32-34` |
| 4 | BOLA: `GET /api/inventory/characters/{characterId}` — no ownership check | ✅ Fixed in `InventoryService.java:55-57` |
| 5 | BOLA: `GET /api/inventory/{id}` — no ownership check | ✅ Fixed in `InventoryService.java:65-67` |
| 6 | BOLA: `DELETE /api/inventory/{id}` — no ownership check | ✅ Fixed in `InventoryService.java:76-78` |
| 7 | BOLA: `GET /api/hunt-sessions/{id}` — no ownership check | ✅ Fixed in `HuntSessionService.java:54-56` |
| 8 | BOLA: Hunt import — no character ownership check | ✅ Fixed in `HuntImportService.java:60-62` |
| 9 | No `.env.example` file | ✅ Created at root with placeholder values |
| 10 | No password minimum length validation | ✅ Fixed in `RegisterRequest.java` — `@Size(min = 8, max = 100)` |
| 11 | Swagger UI publicly accessible in production | ✅ Fixed in `application-prod.properties` — both UI and docs disabled |
| 12 | Weak dev DB password not documented as unsafe for prod | ✅ Fixed — warning in `application.properties:1-2` and `.env.example:9` |
