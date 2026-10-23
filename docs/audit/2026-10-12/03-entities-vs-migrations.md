# 3. Entities vs migrations

## Table, columns, and nullability

All 24 JPA entities have `@Entity` and an `@Table` name corresponding to an existing migration table. Entity column and join-column mappings were compared with migration columns; **no missing entity columns or nullability differences were found**. No declared entity column name was found to differ from the migration column.

| Entity | `@Table` | Migration |
|---|---|---|
| `AcademicYear` | `academic_years` | V003 |
| `Assessment` | `assessments` | V016 |
| `AssessmentTopic` | `assessment_topics` | V017 |
| `AuditLog` | `audit_log` | V023 |
| `ClassEntity` | `classes` | V006 |
| `ClassLevel` | `class_levels` | V005 |
| `Comment` | `comments` | V020 |
| `GeneratedReport` | `generated_reports` | V022 |
| `Guardian` | `guardians` | V011 |
| `ImportJob` | `import_jobs` | V025 |
| `LoginHistory` | `login_history` | V009 |
| `ReportRequest` | `report_requests` | V021 |
| `School` | `schools` | V001 |
| `SchoolSettings` | `school_settings` | V024 |
| `Score` | `scores` | V018 |
| `ScoreHistory` | `score_history` | V019 |
| `Stream` | `streams` | V007 |
| `Student` | `students` | V010 |
| `Subject` | `subjects` | V012 |
| `TeacherAssignment` | `teacher_assignments` | V015 |
| `Term` | `terms` | V004 |
| `Topic` | `topics` | V014 |
| `User` | `users` | V002 |
| `UserSession` | `user_sessions` | V008 |

**Evidence:** `backend/src/main/java/com/musomi/manager/entity/Student.java` contains `@Table(name = "students")`; `backend/src/main/resources/db/migration/V010__create_students.sql` creates `students` with the corresponding `admission_number` and other entity columns. No issues found for entity table names, column names, or nullability.

## Relationships

Every `@ManyToOne` join column checked resolves to the migration FK column and the target entity table. No incorrect relationship target was found.

**Evidence:** `backend/src/main/java/com/musomi/manager/entity/Score.java` maps `assessment_id` to `Assessment`; `backend/src/main/resources/db/migration/V018__create_scores.sql` states `FOREIGN KEY (assessment_id) REFERENCES assessments(id)`. No issues found for the declared entity relationships.

## Unmapped table and verification limit

The `class_subjects` table has no entity mapping or `@JoinTable` reference in backend entity sources. It is not a mismatch for one of the 24 existing entities, but its runtime use is unknown; determine whether the table is intentionally reserved or should back a model relationship.

**Evidence:** `backend/src/main/resources/db/migration/V013__create_class_subjects.sql` creates `class_subjects`; no Java entity references `class_subjects` or uses `@JoinTable`. **Should be:** either map and use the association or document why it is intentionally unused.

Hibernate schema validation was not verified against a live PostgreSQL database. `mvn clean compile` verifies Java compilation only; the application-context test fails before Hibernate validation because PostgreSQL at `localhost:5432` refuses connections. Do not infer a successful Hibernate `validate` from compilation.
