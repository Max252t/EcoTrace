# EcoTrace Backend

REST API server for **EcoTrace**, the city environmental problem tracking application.
It stores reports and user accounts, issues JWT tokens and exposes a JSON API consumed by the
Android client.

This is a standalone Gradle project — it is not part of the Android build and has its own
`settings.gradle.kts`.

## Tech stack

| Layer | Technology |
|-------|------------|
| Server | Ktor 3.1.3 (Netty) |
| Serialization | kotlinx.serialization |
| Database | PostgreSQL 16 |
| ORM | Exposed 0.61 |
| Connection pool | HikariCP |
| Auth | JWT (HS256) |
| Password hashing | BCrypt (cost 12) |
| Logging | Logback |
| Build | Gradle Kotlin DSL, Ktor plugin (`buildFatJar`) |

Dependencies are wired manually in `Application.module()`; repositories are plain classes with no DI
container. The schema is created on startup by `SchemaUtils.createMissingTablesAndColumns`, so no
migration tool is required.

## Structure

```
backend/
├── src/main/kotlin/com/ecotrace/backend/
│   ├── Application.kt           ← entry point, CORS, call logging, plugin wiring
│   ├── auth/
│   │   └── JwtConfig.kt         ← token generation and verification
│   ├── data/
│   │   ├── db/
│   │   │   ├── DatabaseFactory.kt   ← Hikari pool, Exposed connection, schema creation
│   │   │   ├── Tables.kt            ← users and reports tables
│   │   │   └── DbExtensions.kt
│   │   └── repository/
│   │       ├── ReportsRepositoryImpl.kt
│   │       └── UsersRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Report.kt        ← domain models and DTOs
│   │   │   └── User.kt
│   │   └── repository/
│   │       ├── ReportsRepository.kt
│   │       └── UsersRepository.kt
│   ├── plugins/
│   │   ├── Routing.kt
│   │   ├── Security.kt
│   │   ├── Serialization.kt
│   │   └── StatusPages.kt       ← maps exceptions to JSON error responses
│   └── routes/
│       ├── AuthRoutes.kt
│       └── ReportsRoutes.kt
├── src/main/resources/
│   ├── application.conf         ← port, JWT and database settings
│   └── logback.xml
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts
```

## Quick start

### 1. Run PostgreSQL only

```bash
docker compose up postgres -d
```

### 2. Run the server locally

```bash
./gradlew run
```

The server starts on `http://localhost:8080`.

### 3. Run everything with Docker Compose

```bash
docker compose up --build
```

This builds a fat JAR inside the image and starts the API together with PostgreSQL. The API is
published on port `8080` and the database on `5432`.

## Configuration

Settings are read from `src/main/resources/application.conf` and can be overridden by environment
variables. Copy `.env.example` to `.env` and adjust it, or export the variables directly.

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/ecotrace` | JDBC connection string |
| `DATABASE_USER` | `ecotrace` | Database user |
| `DATABASE_PASSWORD` | `ecotrace` | Database password |
| `JWT_SECRET` | `ecotrace-secret-key-change-in-production` | HS256 signing key — **must** be replaced in production |

Tokens are issued with issuer `ecotrace`, audience `ecotrace-users` and a lifetime of 24 hours, and
carry the `userId`, `email` and `role` claims.

## Data model

`users`

| Column | Type | Notes |
|--------|------|-------|
| `id` | varchar(36) | UUID, primary key |
| `email` | varchar(255) | unique |
| `password_hash` | varchar(255) | BCrypt hash |
| `display_name` | varchar(128) | |
| `role` | varchar(32) | `USER` or `ADMIN` |
| `created_at` | timestamp | |

`reports`

| Column | Type | Notes |
|--------|------|-------|
| `id` | varchar(36) | UUID, primary key |
| `title` | varchar(255) | |
| `description` | text | |
| `type` | varchar(64) | problem type |
| `status` | varchar(64) | report status |
| `latitude` / `longitude` | double | |
| `image_url` | varchar(512) | nullable |
| `author_id` | varchar(36) | references `users.id` |
| `created_at` / `updated_at` | timestamp | |

**Problem types:** `DUMP`, `ROAD_PIT`, `PIPE_RUPTURE`, `FALLEN_TREE`

**Statuses:** `OPEN`, `IN_PROGRESS`, `RESOLVED`

---

## API reference

### Health

```
GET /health
```

```json
{ "status": "ok", "service": "EcoTrace API" }
```

### Auth

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/api/auth/register` | Register a new account |
| `POST` | `/api/auth/login` | Sign in |

**Register body** (password must be at least 6 characters):

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "Ivan"
}
```

**Login body:**

```json
{ "email": "user@example.com", "password": "secret123" }
```

**Response** (`201 Created` for register, `200 OK` for login):

```json
{
  "token": "<JWT>",
  "userId": "uuid",
  "email": "user@example.com",
  "displayName": "Ivan",
  "role": "USER"
}
```

Register returns `409 Conflict` if the email is already taken; login returns `401 Unauthorized` for
invalid credentials.

### Reports

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/reports` | — | List reports, filters: `?type=DUMP&status=OPEN` |
| `GET` | `/api/reports/{id}` | — | Single report |
| `POST` | `/api/reports` | ✅ | Create a report |
| `PATCH` | `/api/reports/{id}/status` | ✅ | Change the status |
| `DELETE` | `/api/reports/{id}` | ✅ | Delete a report |
| `GET` | `/api/users/me/reports` | ✅ | Reports created by the current user |

A new report is always created with status `OPEN` and the author taken from the token. Changing the
status and deleting are allowed only for the author of the report or for a user with the `ADMIN`
role; otherwise the server answers `403 Forbidden`.

**Create body:**

```json
{
  "title": "Dump near the park",
  "description": "Unauthorised waste dump",
  "type": "DUMP",
  "latitude": 55.751244,
  "longitude": 37.618423,
  "imageUrl": null
}
```

**Status update body:**

```json
{ "status": "IN_PROGRESS" }
```

**Report response:**

```json
{
  "id": "uuid",
  "title": "Dump near the park",
  "description": "Unauthorised waste dump",
  "type": "DUMP",
  "status": "OPEN",
  "latitude": 55.751244,
  "longitude": 37.618423,
  "imageUrl": null,
  "authorId": "uuid",
  "createdAt": "2026-04-14T10:15:30Z",
  "updatedAt": "2026-04-14T10:15:30Z"
}
```

**Authorization header:**

```
Authorization: Bearer <token>
```

### Errors

Failures are returned as JSON with a single `error` field:

```json
{ "error": "Report not found" }
```

Malformed bodies and unknown enum values produce `400 Bad Request`, unhandled exceptions produce
`500 Internal Server Error` and are written to the log.

## Connecting the Android client

Point `backend.base.url` in the root `local.properties` at this server:

```
backend.base.url=http://10.0.2.2:8080/
```

`10.0.2.2` is the host machine as seen from the Android emulator. For a physical device use the LAN
address of the host, for example `http://192.168.0.136:8080/`.
