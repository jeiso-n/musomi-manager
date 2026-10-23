# Musomi Manager — Status Report

_Generated: 2026-10-12_

> This file describes current code state. For scope, see MASTER.md §4.

## 1. Repository

The Maven backend is under `backend\`, the desktop application is under `desktop\`, and project documentation is under `docs\`. This status snapshot records component and test counts rather than branch synchronization.

## 2. Actual Stack

Versions below come from `backend\pom.xml` and the requested `dependency:list` command run from `backend\`. The PostgreSQL server image is additionally read from `docker-compose.yml`.

| Technology | Actual on disk | MASTER.md §2 | Comparison |
|---|---|---|---|
| Java | 21 (`java.version` in `backend\pom.xml`) | Java 21 | Match |
| Spring Boot | 4.1.1 parent and resolved starters | Spring Boot 3.2.x | **Mismatch** |
| Spring Security | `spring-security-core` 7.1.1; JWT library JJWT 0.12.6; `BCryptPasswordEncoder` configured | Spring Security + JWT + BCrypt (no versions) | Present; no version specified in MASTER |
| Hibernate / JPA | `hibernate-core` 7.4.5.Final; Spring Data JPA starter declared | Spring Data JPA (Hibernate) | Present; no version specified in MASTER |
| PostgreSQL | JDBC driver 42.7.13; server image `postgres:16` in `docker-compose.yml` (server is not versioned in `pom.xml`) | PostgreSQL 15/16 | Image major matches; server patch version unknown |
| Flyway | `flyway-core` and PostgreSQL database module 12.4.0; Spring Boot Flyway starter 4.1.1 | Flyway (no version) | Present |
| Desktop | JavaFX 21.0.2 in `desktop\pom.xml` | JavaFX 21 | Match |
| Student web | Thymeleaf starter declared; no files under backend templates/static; no HTMX or Tailwind dependency/file found | Thymeleaf + HTMX + Tailwind CSS | **Mismatch:** only Thymeleaf dependency is present |
| Build | Maven | Maven | Match |
| PDF | No OpenPDF or iText dependency in `backend\pom.xml` | OpenPDF or iText | **Mismatch:** absent |
| Excel | No Apache POI dependency in `backend\pom.xml` | Apache POI | **Mismatch:** absent |
| Hosting | `infra\` contains only `.gitkeep`; no Cloudflare Tunnel config found | School server + Cloudflare Tunnel | Deployment setup unknown |
| CI/CD | `.github\workflows\ci.yml` runs backend and desktop `mvn clean verify` on pushes/PRs to `main`; latest run result not inspected | GitHub Actions | Configured |

`spring-boot-starter-webmvc` is declared rather than `spring-boot-starter-web`; the requested dependency filter returned WebMVC 4.1.1.

## 3. Backend — What Exists

| Component | Files | Status |
|---|---|---|
| Entities | **24** | Backend entities |
| Repositories | **24** | Spring Data repositories |
| Services | **22** | Backend services |
| API controllers | **20** | REST API controllers |
| Web controllers | **7** | MVC web controllers |
| Request DTOs | **21** | Request models |
| Response DTOs | **28+** | Response models |

## 4. Migrations

There are **26 migrations**: `V001`–`V025` plus `V099`.

## 5. API Endpoints

The API controller source contains **62 mapping annotations**: 27 `@GetMapping`, 23 `@PostMapping`, 7 `@PutMapping`, and 5 `@DeleteMapping`.

## 6. Tests

**185 passing tests:** 150 backend tests and 35 desktop tests.

## 7. Doc Drift

This report is a component-count snapshot. The API endpoint contract and implementation status should be compared separately; annotation totals count declared mappings and do not imply that every contract requirement is complete.

## 8. Blockers

No blocker was identified for this status update. Release readiness, deployment, and pilot-school operation are outside the component counts recorded here.

## 9. v1 Scope Progress

The component totals and endpoint mapping count above describe implementation breadth, not completion of the v1 acceptance criteria. See MASTER.md §4 for scope.

## 10. Recommended Next 5 Tasks

1. Compare annotated API mappings with API_CONTRACT.md and track any remaining gaps.
2. Run backend and desktop verification in CI and retain the test results with the release record.
3. Verify all migrations against a clean PostgreSQL database.
4. Review authorization and tenant scoping for all protected endpoints.
5. Reassess v1 feature completion against MASTER.md §4 acceptance criteria.
