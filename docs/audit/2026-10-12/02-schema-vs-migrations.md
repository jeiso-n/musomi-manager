# 2. SCHEMA.md vs migrations

## Table and version cross-check

Every table in `docs/shared/SCHEMA.md` §4 has a corresponding migration. The 25 DDL migrations are:

| Table | Migration | Table | Migration |
|---|---|---|---|
| `schools` | V001 | `users` | V002 |
| `academic_years` | V003 | `terms` | V004 |
| `class_levels` | V005 | `classes` | V006 |
| `streams` | V007 | `user_sessions` | V008 |
| `login_history` | V009 | `students` | V010 |
| `guardians` | V011 | `subjects` | V012 |
| `class_subjects` | V013 | `topics` | V014 |
| `teacher_assignments` | V015 | `assessments` | V016 |
| `assessment_topics` | V017 | `scores` | V018 |
| `score_history` | V019 | `comments` | V020 |
| `report_requests` | V021 | `generated_reports` | V022 |
| `audit_log` | V023 | `school_settings` | V024 |
| `import_jobs` | V025 |  |  |

`V099__test_data.sql` is an additional seed migration, not a 26th schema table migration.

**Evidence:** `docs/shared/SCHEMA.md` says `**25 tables in v1.**`; `backend/src/main/resources/db/migration/V025__create_import_jobs.sql` creates `import_jobs`.

## Columns, types, nullability

Automated comparison of the 25 section-4 column lists against `CREATE TABLE` definitions found **no missing/extra columns and no declared type or nullability mismatches**. The listed SQL types, lengths, decimal precision, and nullability agree.

**Evidence:** `docs/shared/SCHEMA.md` §4 defines `score | DECIMAL(6,2) | Yes`; `backend/src/main/resources/db/migration/V018__create_scores.sql` defines `score DECIMAL(6,2)` as nullable. No issues found for table coverage or column/type/nullability parity.

## Constraint and index mismatches

1. **The `academic_years` section contains copied `users` constraints and indexes.** In `docs/shared/SCHEMA.md`, the `academic_years` section says `FK: fk_users_school`, `Unique: uq_users_school_username (school_id, username)`, and indexes `idx_users_*`. The actual migration is `backend/src/main/resources/db/migration/V003__create_academic_years.sql`, which declares `fk_academic_years_school_id`, unique `uk_academic_years_school_year (school_id, year)`, and indexes `idx_academic_years_school_id` and `idx_academic_years_current`. **Should be:** document the academic-year FK, per-school year uniqueness, and its actual indexes.
2. **`users` constraint/index names differ, while the semantics match.** `docs/shared/SCHEMA.md` lists `fk_users_school_id`, `uk_users_school_username`, `ck_users_role`, and `idx_users_active`; `backend/src/main/resources/db/migration/V002__create_users.sql` uses `fk_users_school`, `uq_users_school_username`, `chk_users_role`, and `idx_users_school_active`. The schema document itself notes `V002 predates the naming standard in DATABASE.md §4`. **Should be:** either align the documentation with the deployed V002 names or explicitly retain this naming exception. The FK, unique tuple, check condition, and active-school index semantics are present.
3. **`school_settings` names an index not separately created.** `docs/shared/SCHEMA.md` lists `idx_school_settings_school_id`, while `backend/src/main/resources/db/migration/V024__create_school_settings.sql` has no `CREATE INDEX` statement. It does declare `CONSTRAINT uk_school_settings_school UNIQUE (school_id)`, which creates a unique index that supports lookup by `school_id`. **Should be:** remove/document the redundant named index or add it only if an additional non-unique index is intentional.

## Constraints and indexes otherwise

No other documented constraints or indexes were found missing from their migrations. Every non-`schools` table stores `school_id`, consistent with the final corrected school-scoping note in SCHEMA §3.
