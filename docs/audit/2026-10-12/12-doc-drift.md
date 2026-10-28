# 12. Documentation drift

## `docs/STATUS.md` is stale against the current repository

The status report claims the project has only two entities and one API controller, while current inventory has 24 entities and 20 API controllers.

**Evidence:** `docs/STATUS.md` says `| Entities | **2:** School, User ... |` and `| API controllers | **1:** AuthApiController ...`; current code includes `backend/src/main/java/com/musomi/manager/entity/Student.java` (`@Entity`, `@Table(name = "students")`) and the inventory lists 20 API controllers. It also says only two of 63 API endpoints are implemented; current route inspection found 54 API endpoint mappings. **Should be:** regenerate or update `docs/STATUS.md` from the current code and clearly date the status baseline.

## API contract vs routes

The contract lists implemented and missing operations together; ten documented API paths are absent and four API routes are undocumented. Examples include contract `POST /auth/change-password`, absent from `backend/src/main/java/com/musomi/manager/controller/api/AuthApiController.java`, and undocumented `GET /admin/login-history`, implemented by `AdminLoginHistoryApiController`. Full path and shape comparison: [04-api-vs-contract.md](04-api-vs-contract.md). **Should be:** maintain one explicit status per route and align method, path, DTO, status, and role.

## Schema documentation vs migrations

SCHEMA.md contains a copied academic-year constraint/index definition that names user constraints/indexes; V003 uses the academic-year definitions. **Evidence:** `docs/shared/SCHEMA.md` §4 `academic_years` lists `fk_users_school` and `uq_users_school_username`; `backend/src/main/resources/db/migration/V003__create_academic_years.sql` creates the academic-year FK/unique constraint/indexes. **Should be:** correct the copied names and keep the table definition aligned with V003. Other table/column/type definitions matched in the comparison; see [02-schema-vs-migrations.md](02-schema-vs-migrations.md).

## “Not yet implemented” annotations

The API contract's “Not yet implemented” tags on documented API routes are generally consistent with the absence of those API mappings. Example: `docs/shared/API_CONTRACT.md` §8 marks student CRUD as “Not yet implemented”; student CRUD mappings were not found, despite a separate student service/import feature. No evidence was found that those tagged API endpoints are implemented at their documented paths.

**Contrary implementation gap:** reports remain marked as supported contract operations but are explicitly unfinished in code. `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java` says `"Report generation is not available yet"` and returns `HttpStatus.NOT_IMPLEMENTED`; `docs/shared/API_CONTRACT.md` §16 documents report generation and PDF download response shapes. **Should be:** either finish these operations or mark them unavailable in the contract.

No other “not yet implemented” annotation proven stale in the inspected documentation. Unknown: whether design/task documentation outside the checked-in docs accurately describes delivery status.
