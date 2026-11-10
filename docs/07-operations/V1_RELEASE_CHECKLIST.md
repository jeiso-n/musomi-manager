# Musomi Manager V1 — Release and Operations Checklist

**Release line:** V1 Core Operations
**Prepared:** 10 November 2026
**Purpose:** Controlled pilot readiness, deployment, backup, recovery, and release sign-off.

## 1. Release scope

V1 delivers administrator and teacher desktop workflows plus a student web portal, backed by one Spring Boot service and PostgreSQL database. It includes school structure, students, teacher assignments, assessments, topic tags, mark entry, publication, student results, audit history, school settings, and term report cards.

Not part of V1: parent portal, offline mode, notifications/messaging, billing, AI guidance, multi-school administration, and advanced analytics.

## 2. Required environment

- Java 21 runtime
- PostgreSQL 16 (private network; never expose the database port to the public internet)
- Spring Boot backend running with the `prod` profile
- HTTPS reverse proxy / TLS in front of the student portal and API
- JavaFX desktop clients configured to use the deployed API URL
- Persistent storage for generated report PDFs and a documented retention policy

## 3. Production environment variables

The `prod` profile requires all five values below. The application must not start with missing production credentials.

| Variable | Required value |
|---|---|
| `DATABASE_URL` | JDBC URL for the production PostgreSQL database |
| `DATABASE_USERNAME` | Dedicated least-privilege application database user |
| `DATABASE_PASSWORD` | Strong unique password, held in a secret store/environment |
| `JWT_SECRET` | Cryptographically random secret, minimum 32 bytes |
| `REPORT_STORAGE_DIR` | Absolute path to persistent PDF storage; writable by the service account |

Also set `SPRING_PROFILES_ACTIVE=prod`. Never commit real credentials, put them in screenshots, or share them in chat. Do not reuse `dev_password` or the development JWT fallback. Keep the JWT secret stable across application restarts; rotate it only with a planned session invalidation.

Production profile behaviour:

- Hibernate schema mode is `validate`; Flyway owns schema changes.
- SQL statement logging is disabled.
- Application logging is INFO by default; Spring Security logs are WARN.
- Missing database credentials, JWT secret, or report-storage directory prevent startup rather than falling back to development values.

## 4. Build and deploy

1. Back up the current database and report-file storage.
2. Verify the intended Git commit and ensure the working tree is clean.
3. Run backend `mvnw clean verify` with JDK 21 and Docker available for Testcontainers.
4. Run desktop `mvnw clean verify` with JDK 21.
5. Build the backend JAR using `mvnw clean package`.
6. Configure production environment variables on the server using a protected service environment file or secret manager. Restrict file permissions to the service account.
7. Deploy the JAR and start it with `SPRING_PROFILES_ACTIVE=prod`.
8. Confirm Flyway migrations complete and Hibernate schema validation succeeds.
9. Verify `GET /actuator/health` returns `{"status":"UP"}` through the intended internal/HTTPS route.
10. Configure the desktop client's API base URL for the same HTTPS deployment. Distribute a tested desktop package.
11. Verify login for administrator, teacher, and student roles, then execute the critical workflow in section 7.

Do not expose actuator environment/configuration endpoints publicly. Only the health endpoint should be exposed to the monitoring path unless a separate security review approves more.

## 5. Backup procedure (PostgreSQL)

Use a unique timestamped backup filename and store it outside the application checkout on a separate protected disk or backup destination. Restrict access because the backup contains personal student information.

For the repository's local Docker database:

```powershell
New-Item -ItemType Directory -Force D:\MusomiBackups | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
docker exec musomi-postgres pg_dump -U musomi -d musomi_manager -Fc -f /tmp/musomi-$stamp.dump
if ($LASTEXITCODE -ne 0) { throw 'Database backup failed' }
docker cp musomi-postgres:/tmp/musomi-$stamp.dump "D:\MusomiBackups\musomi-$stamp.dump"
if (!(Test-Path "D:\MusomiBackups\musomi-$stamp.dump")) { throw 'Backup file was not created' }
Get-FileHash "D:\MusomiBackups\musomi-$stamp.dump" -Algorithm SHA256
# Local default when the backend is launched with backend as its working directory: backend\data\reports
if (Test-Path 'backend\data\reports') { Copy-Item 'backend\data\reports' "D:\MusomiBackups\reports-$stamp" -Recurse }
```

For a production database, run `pg_dump -Fc` from a trusted backup host using the production database's approved credentials and protected storage. Do not put passwords directly in command history. Also back up the entire `REPORT_STORAGE_DIR` tree (generated report PDFs) in the same backup window; database rows alone do not contain the PDF file bytes.

Recommended pilot cadence: daily automated backup, plus a backup immediately before every deployment or migration. Retain encrypted copies off the application server according to the school's data-retention policy.

## 6. Restore verification

A backup is not proven until it has been restored. Perform restore drills against a separate temporary database/server, never over the live school database.

Example for a local Docker restore drill (use a copied backup file):

```powershell
docker exec musomi-postgres createdb -U musomi musomi_restore_verify
Get-Content -Raw 'D:\MusomiBackups\YOUR_BACKUP.dump' | docker exec -i musomi-postgres pg_restore -U musomi -d musomi_restore_verify --no-owner --no-privileges
if ($LASTEXITCODE -ne 0) { throw 'Restore verification failed' }
docker exec musomi-postgres psql -U musomi -d musomi_restore_verify -c '\dt'
```

After checking table presence, compare important row counts and verify the restored application can start against the temporary database. Only then remove the temporary verification database. Never point this procedure at the production database name.

## 7. Critical V1 acceptance workflow

Record pass/fail and evidence for each step on the pilot environment:

- [ ] Admin login, logout, and session expiry behave correctly.
- [ ] Admin creates/updates a student and assigns the correct class/stream.
- [ ] Admin creates teacher accounts and teacher class/subject assignments.
- [ ] Admin provisions student login or sets an individual permanent password.
- [ ] Teacher login shows only assigned classes and subjects.
- [ ] Teacher creates a topic-tagged assessment for the correct class, stream, subject, and term.
- [ ] Teacher enters marks for the full roster; zero is accepted and missing marks remain distinguishable.
- [ ] Invalid scores above the assessment maximum are rejected.
- [ ] Teacher cannot publish while marks are missing; publish succeeds when complete.
- [ ] Student can log in using the correct school and admission number.
- [ ] Student sees only their own published marks and current-term results.
- [ ] Student cannot access another student's record or report by changing a URL/identifier.
- [ ] Admin/teacher generates an individual report card and verifies its PDF contents.
- [ ] Student downloads their own generated report card.
- [ ] Key mark, publication, user, and settings changes are represented in audit history.
- [ ] Application and database restart without losing persisted records.
- [ ] A database backup is created and restored successfully in an isolated environment.

Do not mark the pilot acceptance workflow complete based only on unit tests or screenshots from seeded test data. Record the school, build commit, test account roles (not passwords), date, and evidence.

## 8. Rollback

- Keep the previously deployed JAR and its commit identifier until the new build passes acceptance checks.
- If application startup or workflow checks fail, stop the new process and restore the previous JAR/configuration.
- Database migrations may be forward-only. Take a verified backup before deployment; do not manually edit Flyway history or drop schema objects to roll back.
- If a migration is incompatible with the previous application, restore the pre-deployment backup into a recovery database and follow the school's approved outage/recovery plan. Never overwrite production data without an explicit incident decision.

## 9. Release evidence and sign-off

- [ ] Backend verification report attached to release notes.
- [ ] Desktop verification report attached to release notes.
- [ ] CI on the release commit is green.
- [ ] No known critical/high defects remain open.
- [ ] Production configuration reviewed without exposing secret values.
- [ ] Backup and restore drill completed.
- [ ] Critical acceptance workflow completed on the pilot environment.
- [ ] Admin, teacher, and student quick-start guides reviewed by real users.
- [ ] Pilot owner approves the release and support contact is known.

A successful local build means the code is buildable. It does not, by itself, certify a production deployment or a real-school pilot. Record those as separate sign-off items.
