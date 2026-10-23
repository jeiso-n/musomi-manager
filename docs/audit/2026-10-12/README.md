# Musomi Manager audit - 2026-10-12

## Executive summary

Musomi Manager has a substantial Java backend and JavaFX desktop application, with all 25 schema tables migrated and most main admin/teacher API routes now implemented. However, a successful compile is not evidence of a usable pilot: report generation and downloads return `501`, student-facing templates still render hardcoded sample data, controllers pass hardcoded school/user IDs, and audit writes are not connected to state changes. The backend test command ran 133 tests but could not load the application-context test because PostgreSQL was not listening on `localhost:5432`; the separate desktop suite passed 35 tests. No existing file was changed by this audit.

## Key numbers

| Measure | Result |
|---|---:|
| Tracked Java files / physical lines | 302 / 21,055 |
| Backend entities / migrations | 24 / 25 schema-table migrations (+ V099 seed data) |
| Repositories / services | 24 / 22 |
| API / web controllers | 20 / 7 |
| Request / response DTOs | 21 / 28 |
| Test source files | 28 (24 backend, 4 desktop) |
| Tests executed | 168 total across both modules |
| Desktop FXML / JavaFX controller files | 26 / 41 |
| v1 implementation estimate | 35% |

Evidence: `backend/pom.xml` contains `<java.version>21</java.version>` and backend build output compiled 178 main source files; `desktop/pom.xml` configures JavaFX 21. Full inventories and test counts are in [01-inventory.md](01-inventory.md) and [07-tests-audit.md](07-tests-audit.md).

## Top 5 findings

1. **Reports cannot be generated or downloaded.** `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java` contains `throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Report generation is not available yet");`; both report generation and download paths are not operational.
2. **Student portal pages show fabricated data.** `backend/src/main/java/com/musomi/manager/controller/web/StudentMarksWebController.java` builds sample records with `Map.of("id", "m1", "title", "Mid-term Test"...);`; student data must come from the authenticated student's records.
3. **Authenticated user identity is replaced with constants.** `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java` declares `private static final Long TEACHER_ID = 1L;`; score writes and assignment checks must use the authenticated teacher identity.
4. **Audit log is queryable but state changes do not call the writer.** `backend/src/main/java/com/musomi/manager/service/AuditService.java` defines `public void log(...)`, but no other production Java source calls `auditService.log(...)`; key changes should persist audit events.
5. **The test suite is not green in the current environment.** `backend/target/surefire-reports/com.musomi.manager.MusomiManagerApplicationTests.txt` reports `Connection to localhost:5432 refused`; run this integration test with PostgreSQL available and make CI-equivalent verification repeatable.

## v1 progress

**Estimate: 35% complete, based on implemented end-to-end capability rather than file count.** Backend entities, schema, service logic, and many REST routes exist, but core user journeys remain partial or blocked by hardcoded identities, mock student pages, missing CRUD/API routes, and absent PDF generation. No evidence of a deployed pilot school or completed acceptance test was found. See [13-scope-progress.md](13-scope-progress.md).

## Top 5 recommendations

1. Resolve authenticated user and school context in every controller instead of fixed IDs; evidence: `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java` says `private static final Long TEACHER_ID = 1L;`.
2. Implement report-card rendering, storage, and downloads; evidence: `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java` says `HttpStatus.NOT_IMPLEMENTED`.
3. Replace student page fixtures with service-backed data and constrain pages to the student's own records; evidence: `backend/src/main/java/com/musomi/manager/controller/web/StudentDashboardWebController.java` says `model.addAttribute("firstName", "Sarah");`.
4. Add audit writer calls to every key state change; evidence: `backend/src/main/java/com/musomi/manager/controller/api/AdminUserApiController.java` still has `// TODO(backend): audit_log when V023 lands`.
5. Make PostgreSQL-backed integration tests run locally and in CI, then add coverage for the complete mark-to-report-to-student flow; evidence: `backend/src/test/java/com/musomi/manager/MusomiManagerApplicationTests.java` is `@SpringBootTest` and the run failed connecting to localhost PostgreSQL.

## Build and test result

- `backend`: `mvn clean compile` passed.
- `desktop`: `mvn clean compile` passed.
- Backend `mvn test`: Tests run 133, Failures 0, Errors 1, Skipped 0.
- Desktop `mvn test`: Tests run 35, Failures 0, Errors 0, Skipped 0.
- Combined test execution: Tests run 168, Failures 0, Errors 1, Skipped 0.

All Maven commands used the cached Maven binary requested by the audit; `mvnw` was not run. The backend test error was an unavailable PostgreSQL service, not a reported assertion failure.

## Worktree note

At audit start, `git status --short` already showed `M ../.idea/compiler.xml`. It was left untouched. All audit artifacts are in this folder; no commit or push was made.
