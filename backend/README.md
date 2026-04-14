# EcoTrace Backend

REST API сервер для приложения **EcoTrace** — мониторинга экологических проблем города.

## Стек

| Слой | Технология |
|------|-----------|
| Сервер | Ktor 3 (Netty) |
| Сериализация | kotlinx.serialization |
| База данных | PostgreSQL 16 |
| ORM | Exposed |
| Connection pool | HikariCP |
| Auth | JWT (HS256) |
| Хэширование паролей | BCrypt |
| DI | Koin |
| Логирование | Logback |

## Структура

```
backend/
├── src/main/kotlin/com/ecotrace/backend/
│   ├── Application.kt           ← точка входа
│   ├── auth/
│   │   └── JwtConfig.kt
│   ├── data/
│   │   ├── db/
│   │   │   ├── DatabaseFactory.kt
│   │   │   ├── Tables.kt
│   │   │   └── DbExtensions.kt
│   │   └── repository/
│   │       ├── ReportsRepositoryImpl.kt
│   │       └── UsersRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Report.kt        ← модели + DTO
│   │   │   └── User.kt
│   │   └── repository/
│   │       ├── ReportsRepository.kt
│   │       └── UsersRepository.kt
│   ├── plugins/
│   │   ├── Routing.kt
│   │   ├── Security.kt
│   │   ├── Serialization.kt
│   │   └── StatusPages.kt
│   └── routes/
│       ├── AuthRoutes.kt
│       └── ReportsRoutes.kt
├── src/main/resources/
│   ├── application.conf
│   └── logback.xml
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts
```

## Быстрый старт

### 1. Запустить только PostgreSQL

```bash
cd backend
docker compose up postgres -d
```

### 2. Запустить сервер локально

```bash
./gradlew run
```

Сервер стартует на `http://localhost:8080`

### 3. Запустить всё через Docker Compose

```bash
docker compose up --build
```

---

## API Reference

### Health

```
GET /health
```

### Auth

| Метод | URL | Описание |
|-------|-----|----------|
| `POST` | `/api/auth/register` | Регистрация |
| `POST` | `/api/auth/login` | Вход |

**Register body:**
```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "Иван"
}
```

**Login body:**
```json
{ "email": "user@example.com", "password": "secret123" }
```

**Response:**
```json
{
  "token": "<JWT>",
  "userId": "uuid",
  "email": "user@example.com",
  "displayName": "Иван",
  "role": "USER"
}
```

### Reports

| Метод | URL | Auth | Описание |
|-------|-----|------|----------|
| `GET` | `/api/reports` | — | Список (фильтры: `?type=DUMP&status=OPEN`) |
| `GET` | `/api/reports/{id}` | — | Конкретный отчёт |
| `POST` | `/api/reports` | ✅ | Создать отчёт |
| `PATCH` | `/api/reports/{id}/status` | ✅ | Изменить статус |
| `DELETE` | `/api/reports/{id}` | ✅ | Удалить отчёт |
| `GET` | `/api/users/me/reports` | ✅ | Мои отчёты |

**Типы проблем:** `DUMP`, `ROAD_PIT`, `PIPE_RUPTURE`, `FALLEN_TREE`

**Статусы:** `OPEN`, `IN_PROGRESS`, `RESOLVED`

**Create body:**
```json
{
  "title": "Свалка у парка",
  "description": "Несанкционированная свалка",
  "type": "DUMP",
  "latitude": 55.751244,
  "longitude": 37.618423,
  "imageUrl": null
}
```

**Authorization header:**
```
Authorization: Bearer <token>
```
