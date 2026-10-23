# 11. Build and CI

## Local Maven validation

All Maven commands were run via the requested cached `mvn.cmd`; `mvnw` was not run.

| Module | Command | Result |
|---|---|---|
| Backend | cached Maven `clean compile` | Passed |
| Desktop | cached Maven `clean compile` | Passed |
| Backend | cached Maven `test` | 133 tests, 0 failures, 1 error, 0 skipped; Spring context test could not connect to PostgreSQL on `localhost:5432` |
| Desktop | cached Maven `test` | 35 tests, 0 failures, 0 errors, 0 skipped |

The backend test error is environment-dependent, but the requested full backend test run did not pass. The full test results and source test inventory are in [07-tests-audit.md](07-tests-audit.md).

## CI

`.github/workflows/ci.yml` exists. It runs on pushes and pull requests to `main`, configures Java 21 and PostgreSQL 16, then runs backend and desktop `mvn clean verify -B`.

**Evidence:** `.github/workflows/ci.yml` includes `image: postgres:16`, `java-version: '21'`, `run: cd backend && mvn clean verify -B`, and `run: cd desktop && mvn clean verify -B`.

CI execution status for this audit is **unknown**; the workflow was inspected, not dispatched.

## Desktop JavaFX plugin

The desktop POM includes `org.openjfx:javafx-maven-plugin` version `0.0.8`, with main class `com.musomi.desktop.DesktopApplication`.

**Evidence:** `desktop/pom.xml` contains `<artifactId>javafx-maven-plugin</artifactId>`. No issues found for the requested plugin check.
