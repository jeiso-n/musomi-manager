# Musomi Manager — Current Project Status

_Last reviewed: 10 November 2026_
_Branch: main_
_Release assessment: V1 release candidate; pilot acceptance and deployment sign-off remain._

> This file supersedes the October 23, 2026 status snapshot. Older audit reports remain historical records and must not be treated as current implementation status.

## 1. Executive summary

Musomi Manager now has a substantial, connected V1 implementation across the Spring Boot backend, JavaFX administrator/teacher desktop client, and student web portal. The previous report's statements that the student portal was mostly demo content and that Admin Users/Classes/Subjects/Teacher Assignments were mock-only are outdated.

Fresh verification on 10 November 2026:

| Module | Verification | Result |
|---|---|---|
| Backend | `mvnw clean verify`, JDK 21, Testcontainers/PostgreSQL 16 | **178 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS** |
| Desktop | `mvnw clean verify`, JDK 21 | **61 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS** |
| Database migrations | Fresh Testcontainers PostgreSQL databases | Migrations through V101 applied in integration runs |
| Backup and restore | Local PostgreSQL custom-format dump restored into isolated temporary database | **Passed** |
| Git | Final pre-release review | Changes pending commit; no force-push or history rewrite |

## 2. Implemented V1 areas

- Role-based administrator, teacher, and student authentication.
- School academic years, terms, classes, streams, subjects, topics, and settings.
- Student CRUD, spreadsheet import support, account provisioning, and individual password reset.
- Teacher accounts and class/subject/stream assignments.
- Teacher classes, assessment creation/editing/review/publishing, mark grids, batch mark entry, score validation, and audit history.
- Student portal dashboard, marks, subject detail, results, profile, password changes, and report-card downloads.
- Report generation, individual PDF downloads, and class performance summaries.
- Desktop profile/account settings and global search.
- School-scoped student login and student data access.

The feature list describes implemented code paths; it is not a claim that every UI workflow has been manually accepted by a pilot school.

## 3. Release-hardening changes on this review

- Production profile now requires `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, and `REPORT_STORAGE_DIR`; no development fallbacks are defined in the production profile. Generated PDF reports now use the configurable storage directory.
- Production disables SQL statement logging and uses INFO application logging / WARN Spring Security logging.
- Default SQL statement logging was disabled and application logging reduced from DEBUG to INFO.
- Re-enabled the previously skipped published-assessment integration test. The test now loads the real seeded roster, saves marks through the batch API, publishes the assessment, and verifies that editing a published assessment is rejected.
- Replaced the incomplete README with local setup, build, run, and production-configuration instructions.
- Added administrator, teacher, and student quick guides.
- Added a V1 release, deployment, backup, restore, rollback, and acceptance checklist.

## 4. Backup and restore evidence

A backup of the current local development database was created outside the repository:

- File: `D:\MusomiBackups\musomi-20261110-133000.dump`
- SHA-256: `F179F2337CE052054BF29418C0B4153EEB1E6AF287D798266254BD3AA88D0E44`
- Restored table count: 27
- Restored student count: 51
- Latest Flyway migration: V101
- Restore target: isolated temporary verification database, removed after the successful check.
- Original `musomi_manager` database and its persistent Docker volume were not reset or modified by the restore drill.

The backup contains personal student data. Keep it private and protected; do not commit it or share it publicly.

## 5. Remaining V1 release gates

These are not all code defects; several require a real deployment or real-user environment.

1. Execute the complete admin → teacher assignment → assessment → mark entry → publish → student results → report download flow on the running application with authorized accounts.
2. Verify administrator, teacher, and student access boundaries in the actual deployed environment.
3. Confirm production secrets and database configuration on the actual pilot server. No school server or production database was provided during this review.
4. Configure and test HTTPS, desktop API URL, report-file persistence, and a distributable Windows desktop package for the pilot environment.
5. Schedule automated backups and perform a restore drill on the pilot server.
6. Obtain pilot-school acceptance and user review of the quick guides.

Class-summary PDF export and a desktop stream filter in the Reports screen remain known reporting enhancements. Decide whether they are required for the first pilot; do not silently represent them as complete.

## 6. How to run locally

See the repository README. Start PostgreSQL with `docker compose up -d postgres`, run the backend from `backend\` using `mvnw.cmd spring-boot:run`, and run the desktop from `desktop\` using `mvnw.cmd javafx:run`. Student portal: `http://localhost:8080/login`.

## 7. Release decision

**Current decision: V1 release candidate, not yet production/pilot signed off.**

The backend and desktop verification suites now pass with zero skipped tests, and a local backup has been restored successfully. Final V1 sign-off requires the real application acceptance workflow and deployment gates in `docs/07-operations/V1_RELEASE_CHECKLIST.md`.
