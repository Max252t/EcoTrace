# EcoTrace

Android application for reporting and tracking city environmental problems on a map: illegal dumps,
road pits, pipe ruptures and fallen trees. Users mark a problem on the map, attach a photo and
follow its status until it is resolved.

The repository contains two independent Gradle projects:

| Project | Path | Description |
|---------|------|-------------|
| Android client | `app/` | Jetpack Compose application with Yandex MapKit |
| REST API server | `backend/` | Ktor service backed by PostgreSQL — see [backend/README.md](backend/README.md) |

## Features

- Map of reports with colour-coded markers by status (open, in progress, resolved)
- Long press on the map creates a new report at that point
- Report creation with a photo from the camera or gallery, problem type and manual location picking
- Filtering by problem type and status
- Personal list of reports with status change and deletion
- Email and password authentication with a JWT session stored on the device
- Offline-first storage: reports are kept in Room and synchronised with the server when it is reachable
- Route to a problem through Yandex Maps or any installed navigation app
- Russian and English localisation, system / light / dark theme, both persisted on the device

## Tech stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Maps | Yandex MapKit 4.26 (lite) |
| DI | Dagger 2 |
| Local storage | Room, SharedPreferences |
| Network | Retrofit 2, OkHttp logging interceptor, Gson |
| Async | Kotlin Coroutines, Flow |
| Images | Coil |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |

Minimum SDK 26, compile and target SDK 36, Java 11 bytecode, Kotlin 2.0.21, AGP 9.1.0.

## Architecture

The client follows a three-layer Clean Architecture split with unidirectional data flow
(`Screen → ViewModel → UseCase → Repository`).

```
app/src/main/java/com/topit/ecotrace/
├── data/
│   ├── local/          Room database, DAO, entities, session storage
│   ├── mapper/         DTO ↔ entity ↔ domain model mapping
│   ├── remote/         Retrofit APIs and remote data sources
│   └── repository/     Repository implementations
├── di/                 Dagger component and modules
├── domain/
│   ├── model/          Domain models (Report, ProblemType, ReportStatus, ReportFilter)
│   ├── repository/     Repository interfaces
│   └── usecase/        Use cases (add, delete, sync, query reports)
├── presentation/
│   ├── map/            Yandex MapKit composable and marker rendering
│   ├── navigation/     Route definitions
│   ├── screens/        Compose screens
│   └── viewmodel/      ViewModels and factory
└── ui/                 Theme, typography, colours, app settings and strings
```

`OfflineFirstReportsRepository` is the single source of truth: reads always come from Room, writes are
stored locally as unsynced and pushed to the API on the next synchronisation pass.

## Getting started

### Prerequisites

- Android Studio Ladybug or newer, or the Android SDK with command line tools
- JDK 17 (used by Gradle; the app itself compiles to Java 11 bytecode)
- A Yandex MapKit API key — get one at [developer.tech.yandex.ru](https://developer.tech.yandex.ru/)
- A running EcoTrace backend (see [backend/README.md](backend/README.md))

### Configuration

All environment-specific values live in `local.properties`, which is not tracked by Git. Copy the
template and fill it in:

```bash
cp local.properties.example local.properties
```

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `sdk.dir` | yes | — | Path to the Android SDK, usually written by Android Studio |
| `yandex.maps.api.key` | yes | empty | Yandex MapKit key, exposed as `BuildConfig.YANDEX_MAPS_API_KEY` |
| `backend.base.url` | no | `http://10.0.2.2:8080/` | Base URL of the REST API, exposed as `BuildConfig.BACKEND_BASE_URL` |

`backend.base.url` is read in `app/build.gradle.kts` and injected into Retrofit through
`BuildConfig`. A trailing slash is added automatically if it is missing. Use:

- `http://10.0.2.2:8080/` for a server running on the host machine, seen from an emulator
- `http://<host-lan-ip>:8080/` for a physical device on the same network
- `https://<your-domain>/` for a deployed server

Cleartext HTTP traffic is allowed in the manifest so that local development against a plain HTTP
server works out of the box.

### Build and run

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:installDebug
```

Or open the project root in Android Studio and run the `app` configuration.

### Tests

Unit tests (domain models, use cases, route building):

```bash
./gradlew :app:testDebugUnitTest
```

Instrumented UI tests (authentication screens, filters bottom sheet) require a connected device or
a running emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Backend

The API server is a separate Gradle project and is not part of the Android build. Start it with
Docker Compose:

```bash
docker compose -f backend/docker-compose.yml up --build
```

Full documentation, including the API reference, is in [backend/README.md](backend/README.md).

## Contributing

The repository follows GitHub Flow: branch off `main`, keep `main` always working, and open a pull
request with a descriptive title and body. Commit messages follow
[Conventional Commits](https://www.conventionalcommits.org/), for example
`feat(map): add long press report creation`.

## License

MIT — see [LICENSE](LICENSE).
