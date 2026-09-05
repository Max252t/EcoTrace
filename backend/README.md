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
│   │   ├── storage/
│   │   │   └── FileStorage.kt   ← photo storage on the file system
│   │   └── repository/
│   │       ├── AchievementsRepositoryImpl.kt
│   │       ├── ReportsRepositoryImpl.kt
│   │       └── UsersRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Achievement.kt   ← achievement codes and DTOs
│   │   │   ├── AchievementRules.kt  ← statistics, unlock rules, merge logic
│   │   │   ├── Report.kt        ← domain models and DTOs
│   │   │   └── User.kt
│   │   └── repository/
│   │       ├── AchievementsRepository.kt
│   │       ├── ReportsRepository.kt
│   │       └── UsersRepository.kt
│   ├── plugins/
│   │   ├── Routing.kt
│   │   ├── Security.kt
│   │   ├── Serialization.kt
│   │   └── StatusPages.kt       ← maps exceptions to JSON error responses
│   └── routes/
│       ├── AchievementsRoutes.kt
│       ├── AuthRoutes.kt
│       ├── FilesRoutes.kt
│       ├── ReportsRoutes.kt
│       └── UsersRoutes.kt
├── src/test/kotlin/           ← unit tests for the achievement rules
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
docker compose -f backend/docker-compose.yml up postgres -d
```

### 2. Run the server locally

The Gradle wrapper lives in the repository root, so run the backend project with `-p backend`:

```bash
./gradlew -p backend run
```

The server starts on `http://localhost:8080`.

### 3. Run the tests

```bash
./gradlew -p backend test
```

### 4. Run everything with Docker Compose

```bash
docker compose -f backend/docker-compose.yml up --build
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
| `DATABASE_PASSWORD` | — | **Required.** Database password |
| `JWT_SECRET` | — | **Required.** HS256 signing key; the server refuses to start without it |
| `CORS_ALLOWED_HOSTS` | `localhost:8080` | Comma-separated origins allowed by CORS, `*` opens the API to any host |
| `UPLOAD_DIR` | `uploads` | Directory for uploaded photos |
| `MAX_UPLOAD_BYTES` | `10485760` | Maximum size of a single upload |

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

`user_achievements`

| Column | Type | Notes |
|--------|------|-------|
| `user_id` | varchar(36) | references `users.id`, part of the primary key |
| `code` | varchar(64) | achievement code, part of the primary key |
| `unlocked_at` | timestamp | earliest known unlock time, including offline unlocks |

**Problem types:** `DUMP`, `ROAD_PIT`, `PIPE_RUPTURE`, `FALLEN_TREE`

**Statuses:** `OPEN`, `IN_PROGRESS`, `RESOLVED`

**Achievements:** `FIRST_REPORT`, `REPORTER_10`, `FOREST_DEFENDER`, `PROBLEM_SOLVER`, `LEVEL_5`

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

Both endpoints are rate limited to 10 requests per minute per client address, which is what stops
password guessing. The email is normalised before the duplicate check, so `USER@a.com` and
`user@a.com` are the same account. Passwords are 6..72 characters (72 is the BCrypt limit) and the
display name is 1..128.

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
| `GET` | `/api/reports` | — | List reports, filters: `?type=DUMP&status=OPEN`, paging: `?limit=100&offset=0` |
| `GET` | `/api/reports/{id}` | — | Single report |
| `POST` | `/api/reports` | ✅ | Create a report |
| `PATCH` | `/api/reports/{id}/status` | ✅ | Change the status |
| `DELETE` | `/api/reports/{id}` | ✅ | Delete a report |
| `GET` | `/api/users/me/reports` | ✅ | Reports created by the current user |

A new report is always created with status `OPEN` and the author taken from the token. Deleting is
allowed for the author or for an `ADMIN`. Status changes are allowed for the same people, but the
author may only move a report between `OPEN` and `IN_PROGRESS` — only an `ADMIN` may set `RESOLVED`,
so nobody can award themselves eco points and the `PROBLEM_SOLVER` achievement. Anything else answers
`403 Forbidden`.

`imageUrl` is not trusted: it must name a file the same user uploaded through `POST /api/files`,
otherwise the request is rejected with `400 Bad Request`. The stored value is always rebuilt by the
server, so a report can never point at somebody else's photo or at an external host.

`title`, `description` and the coordinates are validated; `GET /api/reports` returns at most
`limit` rows (100 by default, 500 maximum).

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

### Files

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `POST` | `/api/files` | ✅ | Upload a photo as `multipart/form-data`, part name `file` |
| `GET` | `/api/files/{name}` | — | Download a stored photo |

JPEG, PNG and WebP are accepted, up to `MAX_UPLOAD_BYTES` (10 MB by default). The type is decided by
the file signature — the client's content type and file name are ignored, so an executable renamed to
`photo.jpg` is rejected with `415 Unsupported Media Type`. The upload is streamed to disk and aborted
as soon as it passes the limit, which answers `413 Payload Too Large` without buffering the body in
memory. Uploads are recorded per user, and a report may only reference an image its own author
uploaded.

```json
{ "name": "0f1c….jpg", "url": "/api/files/0f1c….jpg" }
```

The stored name is a generated UUID, so a client-supplied file name never reaches the file system.
The returned `url` is relative on purpose: clients resolve it against their own base URL, which keeps
the value in `reports.image_url` portable between environments. Downloads are public because reports
themselves are public and image loaders do not send the `Authorization` header.

Files live in `UPLOAD_DIR` on the server's file system, mounted as the `uploads` Docker volume in
`docker-compose.yml`. Deleting a report also deletes its photo, so the directory does not collect
orphans. Moving to S3-compatible storage later only changes `FileStorage` and the value returned in
`url`.

### Users

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/users/{id}` | ✅ | Public profile of a user, used to show report authors by name |

```json
{ "id": "uuid", "displayName": "Ivan", "role": "USER" }
```

The response deliberately omits the email address. An unknown id returns `404 Not Found`.

### Achievements

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| `GET` | `/api/achievements` | ✅ | Achievements already unlocked by the current user |
| `POST` | `/api/achievements/sync` | ✅ | Merge unlocks reported by a client and return the full list |

Achievements are derived from the user's own reports by `AchievementRules`:

| Code | Condition |
|------|-----------|
| `FIRST_REPORT` | at least one report submitted |
| `REPORTER_10` | 10 reports submitted |
| `FOREST_DEFENDER` | 5 reports of type `FALLEN_TREE` |
| `PROBLEM_SOLVER` | 5 own reports with status `RESOLVED` |
| `LEVEL_5` | 400 eco points |

Eco points are `20` per submitted report plus `30` per resolved report, and every `100` points is one
level, so level 5 starts at 400 points.

The sync endpoint exists because the Android client unlocks achievements offline. The server never
trusts a claim blindly: it re-evaluates the rules against its own reports and keeps a claimed unlock
only if the condition currently holds (or the achievement is already stored). For accepted claims the
earliest unlock time wins, so an achievement earned offline keeps the moment it was actually earned;
times in the future are clamped to the current time. Eligible achievements the client did not claim
are unlocked as well, and stored achievements are never revoked.

**Sync body:**

```json
{
  "achievements": [
    { "code": "FIRST_REPORT", "unlockedAt": "2026-04-14T10:15:30Z" }
  ]
}
```

**Response** (both endpoints):

```json
[
  { "code": "FIRST_REPORT", "unlockedAt": "2026-04-14T10:15:30Z" }
]
```

An unknown `code` or an `unlockedAt` that is not an ISO-8601 instant is rejected with
`400 Bad Request`.

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
