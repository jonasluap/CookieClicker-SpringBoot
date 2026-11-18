# Cookie Clicker API — Sprint 1

University project (R3.03 Agile Development) built from the supplied `baseSpringBoot` starter.

**Team:** _Add every team member's full name here before submission._  
**Repository:** https://github.com/jonasluap/CookieClicker-SpringBoot  
**Tech stack:** Java 21 · Spring Boot 4.1.1 · Spring Data JPA · PostgreSQL 16 · Docker Compose · Bruno

## Goal (Sprint 1)

This repository implements a REST API for a future Cookie Clicker browser game. A player earns cookies by clicking and spends them on improvements. This sprint focuses on the backend; **there is no front-end, login/Bearer authentication, advertising network, or external API yet**.

### Features

- REST **CRUD** of players and upgrades: GET all, GET by ID, POST, PUT, DELETE.
- Persist all data in PostgreSQL with Spring Data JPA / Hibernate.
- Click endpoint that credits cookies on the server.
- Purchase endpoint with a balance check, cookie deduction, bonus application, and purchase history.
- Top-10 leaderboard ordered by current cookie balance.
- Input validation, HTTP 200/201/204/400/404/409 and standardized JSON error messages.
- `Player` **One-to-Many** `Purchase`; `Purchase` **Many-to-One** `Player` and `Upgrade`.
- Layered architecture: Controller + DTO -> Service interface / implementation -> Repository -> Entity.
- A Bruno collection (`bruno/CookieClicker-Sprint1/`) covering CRUD and gameplay.

**Important rules:** clients cannot set their own cookie balance or buy upgrades without enough cookies. Each click adds `cookiesPerClick` cookies. An upgrade can grant `clickBonus`, `passiveBonus`, or both. `cookiesPerSecond` is **stored but not yet automatically credited over time**; passive production belongs to a later sprint.

## Directory layout

```
src/main/java/com/basespringboot/
  controller/       PlayerController.java, UpgradeController.java
  dto/              HTTP request and response records (avoid exposing JPA entities)
  entity/           Player.java, Upgrade.java, Purchase.java
  repository/       Spring Data JPA repositories
  service/          PlayerService.java, UpgradeService.java
  service/impl/     PlayerServiceImpl.java, UpgradeServiceImpl.java
  exception/        Error response and centralized exception handling
bruno/CookieClicker-Sprint1/   Bruno requests and local environment
```

## Prerequisites

- Docker Engine + Docker Compose (recommended)
- Java 21 and Maven wrapper for local application development
- Bruno API client for the supplied REST request collection (optional)

## Run — Docker only (recommended)

Run **from the project directory**:

```bash
docker compose up --build -d
docker compose ps
docker compose logs app --tail=40
```

If your Docker installation needs elevated permissions, use `sudo docker compose ...`.

- Spring Boot: `http://localhost:8080`
- PostgreSQL (host port): `localhost:5433`; database `base_db`
- Adminer: `http://localhost:8081`
  - System: `PostgreSQL`
  - Server: `postgres`
  - Username: `postgres`
  - Password: `postgrespassword`
  - Database: `base_db`

To stop without deleting the database volume:

```bash
docker compose down
```

**Do not run Spring Boot from IntelliJ and the `app` Docker service simultaneously on port 8080.** For IntelliJ development, run only PostgreSQL and Adminer in Docker, then start Java locally:

```bash
docker compose up -d postgres adminer
./mvnw spring-boot:run
```

When you change Java code in a running Docker app, rebuild it with `docker compose up --build -d` (the container does not automatically pick up your source changes).

`spring.jpa.hibernate.ddl-auto=update` is used for **development only**; data persists between normal restarts. Use real schema migrations (Flyway/Liquibase) for production. `docker compose down -v` permanently deletes the database volume.

> Security: the default database username/password are **development-only demo credentials**. Change them and use managed secrets before any public deployment. There is **no authentication in Sprint 1**: run this project locally.

## REST endpoints

### Players

| HTTP | Path | Response | Description |
|---|---|---|---|
| POST | `/api/players` | 201 | Create a player |
| GET | `/api/players` | 200 | List all players |
| GET | `/api/players/{id}` | 200 | Get one player |
| PUT | `/api/players/{id}` | 200 | Update player username |
| DELETE | `/api/players/{id}` | 204 | Delete player and its purchase history |
| POST | `/api/players/{id}/click` | 200 | Earn `cookiesPerClick` cookies |
| GET | `/api/players/leaderboard` | 200 | Top ten players by current balance |
| GET | `/api/players/{id}/purchases` | 200 | Purchase history |
| POST | `/api/players/{playerId}/upgrades/{upgradeId}/purchase` | 200 | Buy upgrade |

**Player POST/PUT JSON:**

```json
{ "username": "jonas" }
```

The username must not be blank, cannot exceed 32 characters, and must be unique (case-insensitively checked by the service). A player starts with 0 cookies, 1 cookie/click, 0 passive cookies/second.

### Upgrades

| HTTP | Path | Response | Description |
|---|---|---|---|
| POST | `/api/upgrades` | 201 | Create an upgrade |
| GET | `/api/upgrades` | 200 | List upgrades |
| GET | `/api/upgrades/{id}` | 200 | Get one upgrade |
| PUT | `/api/upgrades/{id}` | 200 | Replace its fields |
| DELETE | `/api/upgrades/{id}` | 204 | Delete if not previously purchased |

**Upgrade POST/PUT JSON:**

```json
{
  "name": "Cursor",
  "description": "Adds two cookies per click",
  "price": 2,
  "clickBonus": 2,
  "passiveBonus": 0
}
```

`price` must be greater than zero. Bonuses cannot be negative, and at least one must be positive. An upgrade can be purchased multiple times for a fixed price. An upgrade with existing purchases cannot be deleted (HTTP 409), to preserve purchase history.

### Sample use (curl)

```bash
# 1. Create a player; note the returned id.
curl -i -X POST http://localhost:8080/api/players \
  -H 'Content-Type: application/json' \
  -d '{"username":"jonas"}'

# 2. Create a cheap upgrade; note its id.
curl -i -X POST http://localhost:8080/api/upgrades \
  -H 'Content-Type: application/json' \
  -d '{"name":"Cursor","description":"More cookies per click","price":2,"clickBonus":2,"passiveBonus":0}'

# Replace 1 with the player ID returned by the API.
curl -i -X POST http://localhost:8080/api/players/1/click
curl -i -X POST http://localhost:8080/api/players/1/click
# Replace 1 with the actual upgrade ID if different.
curl -i -X POST http://localhost:8080/api/players/1/upgrades/1/purchase
curl -i http://localhost:8080/api/players/1
```

### Errors

| Status | Meaning | Example |
|---|---|---|
| 400 | Invalid request | Blank username, negative bonus |
| 404 | Not found | Unknown player or upgrade ID |
| 409 | Conflict | Username taken, not enough cookies, upgrade already purchased and cannot be removed |

Example response:

```json
{
  "timestamp": "2026-11-18T14:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Not enough cookies to purchase this upgrade",
  "path": "/api/players/1/upgrades/1/purchase"
}
```

## Bruno — CRUD verification

1. Open Bruno and choose **Open Collection**.
2. Select `bruno/CookieClicker-Sprint1`.
3. Select the `local` environment (`baseUrl` = `http://localhost:8080`).
4. Run `01 Players/01 Create Player`; it stores the generated `playerId` as a runtime variable.
5. Run `02 Upgrades/01 Create Upgrade`; it stores the generated `upgradeId`.
6. Run the GET/PUT/Click/Purchase requests. Run two clicks before purchase (the sample upgrade costs 2 cookies).
7. Use the DELETE requests last; deleting an upgrade that has been purchased intentionally returns HTTP 409.

Requests that read `{{playerId}}` or `{{upgradeId}}` need the corresponding create request to run first in the same Bruno session. The `local` environment also provides fallback values of 1; adjust them if needed.

## Automated tests

```bash
./mvnw test
```

The included service unit tests use Mockito and do not need a running PostgreSQL instance. Once the Docker API is running, you can also check the whole CRUD/gameplay workflow using Python's standard library:

```bash
python3 scripts/smoke_test.py
```

This smoke test creates disposable sample data and cleans it up. Full formal HTTP + PostgreSQL integration tests and load testing are future-sprint improvements.

## Agile organization / submission checklist

- [ ] Add **all students' real first names and surnames** to this README.
- [ ] Create a GitHub Project board with cards for player CRUD, upgrade CRUD, purchases, Bruno, documentation and testing; assign owners and update status. See `docs/SPRINT1_BOARD.md`.
- [ ] Push commits regularly and clearly (`feat: add player CRUD`, `feat: add upgrades`, etc.).
- [ ] Commit the Bruno collection with the source code.
- [ ] Publish this repository and share its URL with the instructor / on Discord according to the course instructions.
- [ ] Be ready to explain controllers, DTOs, transactions, JPA relations, status codes and request validation.

### Proposed Sprint 2+

- Sprint 2: signup/login with Bearer authentication and authorization.
- Future: automatic cookie production, front end, external free API, a **simulated** rewarded-ad flow, monitoring and load tests.

The backend is intentionally not publicly exposed until access control is implemented. Third-party advertising platforms have their own policies; the planned reward system is only a future concept.
