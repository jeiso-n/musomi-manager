# Musomi Manager

Musomi Manager is a school learning-record system for Ugandan secondary schools. It manages school structure, student records, teacher assignments, topic-tagged assessments, marks, publication, and term report cards.

**Release line:** V1 — Core Operations
**Runtime:** Java 21, Spring Boot 4, JavaFX 21, PostgreSQL 16
**Frontends:** JavaFX desktop for administrators and teachers; responsive Thymeleaf web portal for students.

## V1 scope

- Role-based login for administrators, teachers, and students
- School setup: academic years, terms, class levels, classes, streams, subjects, and topics
- Student records and spreadsheet import
- Teacher accounts and class/subject assignments
- Topic-tagged assessment creation and draft/published workflow
- Mark entry, validation, history, and audit events
- Student portal for published marks, results, profile, password changes, and report cards
- Individual PDF report cards and class performance summaries
- School settings and grading configuration

AI features, parent access, offline mode, notifications, billing, and advanced analytics are outside V1 scope.

## Requirements

- JDK 21
- Docker Desktop (or a local PostgreSQL 16 server)
- Windows PowerShell or a POSIX shell

## Local development setup (Windows)

1. Start PostgreSQL using the repository's local-development Compose file:

   ```powershell
   docker compose up -d postgres
   docker exec musomi-postgres pg_isready -U musomi -d musomi_manager
   ```

   The Compose credentials and `dev_password` are for local development only. Do not reuse them for a school deployment.

2. Build and test the backend:

   ```powershell
   cd backend
   .\mvnw.cmd clean verify
   ```

3. Run the backend (from the `backend` directory):

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The API and student portal are served on `http://localhost:8080`. Student login: `http://localhost:8080/login`. Health check: `http://localhost:8080/actuator/health`.

4. Build and test the JavaFX desktop application in a second terminal:

   ```powershell
   cd desktop
   .\mvnw.cmd clean verify
   ```

5. Run the desktop application:

   ```powershell
   .\mvnw.cmd javafx:run
   ```

The desktop application connects to the backend configured in its application configuration. Start the backend before signing in from the desktop app.

## Production configuration

Production must use the `prod` Spring profile. The profile requires these environment variables and intentionally has no development fallbacks:

- `DATABASE_URL` — JDBC URL, for example `jdbc:postgresql://db-host:5432/musomi_manager`
- `DATABASE_USERNAME` — dedicated database user
- `DATABASE_PASSWORD` — strong database password
- `JWT_SECRET` — cryptographically random secret of at least 32 bytes; keep it private and stable across restarts
- `REPORT_STORAGE_DIR` — absolute path to persistent storage for generated PDF report cards; the service account must be able to write to it

Example PowerShell configuration (replace all example values; do not commit secrets):

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://YOUR_DB_HOST:5432/musomi_manager'
$env:DATABASE_USERNAME = 'musomi_app'
$env:DATABASE_PASSWORD = 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD'
$env:JWT_SECRET = 'REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG'
$env:REPORT_STORAGE_DIR = 'D:\MusomiData\reports'
$env:SPRING_PROFILES_ACTIVE = 'prod'
```

Build the backend JAR with `cd backend; .\mvnw.cmd clean package`, then run it with `java -jar target\manager-0.0.1-SNAPSHOT.jar`. Configure the JavaFX client's API base URL for the deployed server before distributing the desktop application. The client supports the JVM override `-Dmusomi.api.baseUrl=https://YOUR_HOST/api/v1`; its bundled `config.properties` defaults to localhost for development. Put the application behind HTTPS; do not expose PostgreSQL publicly. The current desktop Maven JAR is a build artifact, not a self-contained Windows installer; use the documented Maven launcher for development until a tested pilot distribution package is produced.

**Do not deploy using the local `docker-compose.yml` password or the default JWT secret.** See [the V1 release and operations checklist](docs/07-operations/V1_RELEASE_CHECKLIST.md) for deployment, backup, restore, and release checks.

## User guides

- [Administrator quick guide](docs/06-user-guides/ADMIN_GUIDE.md)
- [Teacher quick guide](docs/06-user-guides/TEACHER_GUIDE.md)
- [Student portal quick guide](docs/06-user-guides/STUDENT_GUIDE.md)

## Repository layout

- `backend/` — Spring Boot API, student web portal, database migrations
- `desktop/` — JavaFX administrator and teacher application
- `docs/` — architecture, API contract, specialist guides, operations and release documentation
- `docker-compose.yml` — local PostgreSQL development service
- `.github/workflows/ci.yml` — GitHub Actions build and test pipeline

## Quality checks

Run both module verifications before merging or releasing:

```powershell
cd backend; .\mvnw.cmd clean verify
cd ..\desktop; .\mvnw.cmd clean verify
```

The backend integration tests use Testcontainers and require Docker. Never run destructive database reset commands against a school database.
