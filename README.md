# Cookie Clicker — Fournee infinie

University project (R3.03 Agile Development) built from the supplied `baseSpringBoot` starter.

**Team:** _Add every team member's full name here before submission._  
**Repository:** https://github.com/jonasluap/CookieClicker-SpringBoot  
**Tech stack:** Java 21 · Spring Boot 4.1.1 · Spring Data JPA · PostgreSQL 16 · Docker Compose · Bruno

## Tester le jeu dans le navigateur

Avec Java 21, lancez cette commande à la racine du projet.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Quand Spring Boot a démarré, ouvrez [http://localhost:8082/](http://localhost:8082/) et choisissez **Créer un compte**. Spring Boot sert le HTML, le CSS, le JavaScript et l’API à la même adresse.

Le profil `local` crée une base H2 intégrée dans `.local-data/` et conserve la progression entre les redémarrages. Pour le même mode dans IntelliJ, ajoutez `--spring.profiles.active=local` aux arguments de la configuration qui lance `BaseSpringBootApplication`. Docker utilise PostgreSQL et sert le jeu sur le port 8080.

## Goal (Sprints 1–3)

This repository includes a playable French Cookie Clicker browser game and its REST API. A player earns cookies by clicking, collects passive production, buys improvements, and appears on the leaderboard. Progress is saved in PostgreSQL or in the embedded H2 database with the local profile. Accounts use stateless Bearer token authentication.

### Features

- REST **CRUD** of players and upgrades: GET all, GET by ID, POST, PUT, DELETE.
- Persist all data in PostgreSQL with Spring Data JPA / Hibernate.
- Click endpoint that credits cookies on the server.
- Purchase endpoint with a balance check, cookie deduction, bonus application, and purchase history.
- Top-10 leaderboard ordered by current cookie balance.
- Input validation, HTTP 200/201/204/400/404/409 and standardized JSON error messages.
- `Player` **One-to-Many** `Purchase`; `Purchase` **Many-to-One** `Player` and `Upgrade`.
- Layered architecture: Controller + DTO -> Service interface / implementation -> Repository -> Entity.
- Bruno collections for Sprints 1–3, covering CRUD, authenticated gameplay, the web entry point, weather, passive production and Swagger.
- Signup/login returning signed JWT access tokens, validated by Spring Security's OAuth2 Resource Server support.
- Passwords stored as BCrypt hashes; player endpoints require a Bearer token and only the token owner can access that player's account.
- Responsive browser interface served at `http://localhost:8080/`, with account creation, gameplay, upgrades and leaderboard.
- A starter upgrade catalogue is added automatically when the database has no upgrades.

**Important rules:** clients cannot set their own cookie balance or buy upgrades without enough cookies. Each click adds `cookiesPerClick` cookies. Passive production is credited by the game while the player is online. An upgrade can grant `clickBonus`, `passiveBonus`, or both.

### Bonus météo

Dans le jeu, recherchez une ville dans **Météo locale**. Le serveur récupère les conditions auprès d’Open-Meteo et ajoute le bonus à chaque clic et à chaque seconde de production passive.

| Conditions | Cookies supplémentaires par clic et par seconde |
|---|---:|
| Ciel dégagé ou nuageux | +1 |
| Brouillard, pluie ou neige | +2 |
| Orage | +3 |

Le bonus est valable une heure et s’ajoute aux améliorations achetées. Recherchez à nouveau la ville pour actualiser la météo après expiration.

## Directory layout

```
src/main/java/com/basespringboot/
  controller/       AuthController.java, PlayerController.java, UpgradeController.java
  dto/              HTTP request and response records (avoid exposing JPA entities)
  entity/           Player.java, Upgrade.java, Purchase.java
  repository/       Spring Data JPA repositories
  service/          AuthService.java, TokenService.java, PlayerService.java, UpgradeService.java
  service/impl/     PlayerServiceImpl.java, UpgradeServiceImpl.java
  exception/        Error response and centralized exception handling
bruno/CookieClicker-API/       Complete Bruno API collection organized by feature
bruno/CookieClicker-Sprint*/   Bruno test scenarios by sprint
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
- Game: `http://localhost:8080/` (create an account, then click the cookie)
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

> Security: the default database credentials and JWT signing key are **development-only values**. Set managed secrets before any public deployment; use HTTPS outside local development.

### Authentication and authorization (Sprint 2)

Set a unique random signing key through `JWT_SECRET` in every environment. The included fallback is for local development only. Tokens use HS256 and expire after one hour; passwords are BCrypt-hashed. HTTPS is required outside local development. Signup and login are public; all other API routes require `Authorization: Bearer <accessToken>`. A token can access only its own player routes (`/api/players/{id}`, clicks, purchases, and history). `GET /api/players` returns the authenticated player's profile. The leaderboard and upgrade catalogue are visible to authenticated users.

`POST /api/players` has been replaced by account signup so every new player has credentials. Existing Sprint 1 player rows remain in the database, but have no password hash; create a new account or migrate/reset those rows before logging in as them.

Signup request (`POST /api/auth/signup`, returns 201):

```json
{ "username": "jonas", "password": "a-long-password" }
```

Login request (`POST /api/auth/login`, returns 200):

```json
{ "username": "jonas", "password": "a-long-password" }
```

Both return `accessToken`, `tokenType` (`Bearer`), `expiresIn` (seconds), `playerId`, and `username`. Use the returned id for player routes and send the token in the `Authorization` header. Signup requires a nonblank username of at most 32 characters, a password of 8–72 characters, and a case-insensitively unique username. Invalid credentials return 401; attempting another player's route returns 403.

## REST endpoints

### Swagger UI

Ouvre **Fournée infinie — API Cookie Clicker** pour consulter les routes et les tester depuis le navigateur.

- Profil `local` sur [localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html).
- Docker sur [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

Dans **Authentification**, ouvre `POST /api/auth/signup` ou `POST /api/auth/login`, puis clique sur **Try it out** et **Execute**. Copie la valeur d’`accessToken` dans **Authorize**. Swagger ajoute l’en-tête Bearer aux requêtes protégées. Utilise le `playerId` renvoyé dans les routes qui le demandent.

La spécification OpenAPI est disponible sur `/v3/api-docs` au format JSON et sur `/v3/api-docs.yaml` au format YAML.

### Players

| HTTP | Path | Response | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | 201 | Create an account and issue a Bearer token |
| POST | `/api/auth/login` | 200 | Authenticate and issue a Bearer token |
| GET | `/api/players` | 200 | Get the authenticated player's profile |
| GET | `/api/players/{id}` | 200 | Get one player |
| PUT | `/api/players/{id}` | 200 | Update player username |
| DELETE | `/api/players/{id}` | 204 | Delete player and its purchase history |
| POST | `/api/players/{id}/click` | 200 | Earn `cookiesPerClick` cookies |
| POST | `/api/players/{id}/collect` | 200 | Collect `cookiesPerSecond` cookies |
| PUT | `/api/players/{id}/weather` | 200 | Set the weather and production bonus for the player's chosen city (`{"city":"Paris"}`) |
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
# 1. Create an account; copy accessToken and playerId from the response.
curl -i -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"jonas","password":"a-long-password"}'

# 2. Send this header on each protected request.
curl -i http://localhost:8080/api/players \
  -H 'Authorization: Bearer <accessToken>'

# Create upgrades as an authenticated user; note the returned id.
curl -i -X POST http://localhost:8080/api/upgrades \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Cursor","description":"More cookies per click","price":2,"clickBonus":2,"passiveBonus":0}'

# Replace 1 with the player ID returned by the API.
curl -i -X POST http://localhost:8080/api/players/1/click -H 'Authorization: Bearer <accessToken>'
curl -i -X POST http://localhost:8080/api/players/1/click -H 'Authorization: Bearer <accessToken>'
# Replace 1 with the actual upgrade ID if different.
curl -i -X POST http://localhost:8080/api/players/1/upgrades/1/purchase -H 'Authorization: Bearer <accessToken>'
curl -i http://localhost:8080/api/players/1 -H 'Authorization: Bearer <accessToken>'
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

## Bruno — API collections

| Collection | Coverage |
|---|---|
| `bruno/CookieClicker-API` | All 17 application API operations grouped into Authentication, Players, Upgrades, Gameplay and External API. |
| `bruno/CookieClicker-Sprint1` | The original profile, upgrades, clicks, purchases, history and leaderboard scenarios, adapted to the current API. |
| `bruno/CookieClicker-Sprint2` | Signup, login, JWT authentication, CRUD and gameplay. |
| `bruno/CookieClicker-Sprint3` | Game page, Swagger, weather validation, weather bonuses, passive production and saved progress. |

In Bruno, choose **Open Collection** and select a collection folder. Use the `local` environment for the Spring `local` profile on port `8082`. The complete API collection and the Sprint 1 and Sprint 3 collections also provide a `docker` environment for port `8080`.

In **CookieClicker API**, set `playerUsername` and `playerPassword` in the environment, then start with **Signup** for a new account or **Login** for an existing account. Both requests save the JWT and player ID for the other folders. **Create Upgrade** saves the upgrade ID for the catalogue and purchase requests. The folders group related endpoints, so run individual requests in the order of the scenario you want to demonstrate. For example, click twice before buying the default demo upgrade. **Update Weather** uses the environment variable `weatherCity`.

For Sprint 1 and Sprint 3, run the numbered requests in order using **Run Collection** or `bru run --env local` from the collection folder. The first account request creates a unique demo player and passes its ID and JWT to the following requests. The final requests delete the demo data. If you stop a run early, the numbered cleanup requests are available at the end of the same collection.

The Sprint 1 collection uses `POST /api/auth/signup` to create a player. The current `GET /api/players` returns the signed-in player, and the leaderboard route lists the top ten balances. The Sprint 3 weather scenario uses the `weatherCity` environment variable, set to Paris by default, and needs access to Open-Meteo.

### Sprint 2 walkthrough

1. Run `01 Players/01 Create Player` to sign up, or `01 Players/00 Login` if the account already exists. Each stores `playerId` and `accessToken` as runtime variables.
2. Run `02 Upgrades/01 Create Upgrade` to store the generated `upgradeId`.
3. Run the GET/PUT/Click/Purchase requests. Run two clicks before purchase (the sample upgrade costs 2 cookies).
4. Use the DELETE requests last. Deleting an upgrade that has been purchased returns HTTP 409.

Requests that read `{{playerId}}` or `{{upgradeId}}` need the corresponding create request to run first in the same Bruno session. The `local` environment also provides fallback values of 1; adjust them if needed.

## Automated tests

```bash
./mvnw test
```

The included service unit tests use Mockito and do not need a running PostgreSQL instance. With the app running in local mode or in Docker, check the browser assets and authenticated gameplay using Python's standard library.

```bash
python3 scripts/smoke_test.py                        # Docker
python3 scripts/smoke_test.py http://localhost:8082/  # local profile
```

The smoke test creates a test account and upgrade and removes them afterwards. Pass the game URL as an argument if the app runs on a different port.

## Agile organization / submission checklist

- [ ] Add **all students' real first names and surnames** to this README.
- [ ] Create a GitHub Project board with cards for player CRUD, upgrade CRUD, purchases, Bruno, documentation and testing; assign owners and update status. See `docs/SPRINT1_BOARD.md`.
- [ ] Push commits regularly and clearly (`feat: add player CRUD`, `feat: add upgrades`, etc.).
- [ ] Commit the Bruno collections with the source code.
- [ ] Publish this repository and share its URL with the instructor / on Discord according to the course instructions.
- [ ] Be ready to explain controllers, DTOs, transactions, JPA relations, status codes and request validation.

### Future improvements

- A **simulated** rewarded-ad flow, monitoring and load tests.

Third-party advertising platforms have their own policies; the planned reward system is only a future concept.
