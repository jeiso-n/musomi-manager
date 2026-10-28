# 1. Project inventory

Counts below use tracked source files and named production package folders. Full tracked Java total includes the checked-in Maven wrapper downloader Java source.

**Evidence:** `backend/src/main/java/com/musomi/manager/entity/Student.java` begins `@Entity` and `@Table(name = "students")`; `backend/src/main/resources/db/migration/V010__create_students.sql` is a migration; test examples are under `backend/src/test/java`.

## Migrations

25 table migrations plus one test-data SQL file (26 `*.sql` files total):

`V001__create_schools.sql`, `V002__create_users.sql`, `V003__create_academic_years.sql`, `V004__create_terms.sql`, `V005__create_class_levels.sql`, `V006__create_classes.sql`, `V007__create_streams.sql`, `V008__create_user_sessions.sql`, `V009__create_login_history.sql`, `V010__create_students.sql`, `V011__create_guardians.sql`, `V012__create_subjects.sql`, `V013__create_class_subjects.sql`, `V014__create_topics.sql`, `V015__create_teacher_assignments.sql`, `V016__create_assessments.sql`, `V017__create_assessment_topics.sql`, `V018__create_scores.sql`, `V019__create_score_history.sql`, `V020__create_comments.sql`, `V021__create_report_requests.sql`, `V022__create_generated_reports.sql`, `V023__create_audit_log.sql`, `V024__create_school_settings.sql`, `V025__create_import_jobs.sql`, `V099__test_data.sql`.

Paths: `backend/src/main/resources/db/migration/*.sql`.

## Backend application types

| Category | Count | File names |
|---|---:|---|
| Entities | 24 | `AcademicYear`, `Assessment`, `AssessmentTopic`, `AuditLog`, `ClassEntity`, `ClassLevel`, `Comment`, `GeneratedReport`, `Guardian`, `ImportJob`, `LoginHistory`, `ReportRequest`, `School`, `SchoolSettings`, `Score`, `ScoreHistory`, `Stream`, `Student`, `Subject`, `TeacherAssignment`, `Term`, `Topic`, `User`, `UserSession` |
| Repositories | 24 | `AcademicYearRepository`, `AssessmentRepository`, `AssessmentTopicRepository`, `AuditLogRepository`, `ClassLevelRepository`, `ClassRepository`, `CommentRepository`, `GeneratedReportRepository`, `GuardianRepository`, `ImportJobRepository`, `LoginHistoryRepository`, `ReportRequestRepository`, `SchoolRepository`, `SchoolSettingsRepository`, `ScoreHistoryRepository`, `ScoreRepository`, `StreamRepository`, `StudentRepository`, `SubjectRepository`, `TeacherAssignmentRepository`, `TermRepository`, `TopicRepository`, `UserRepository`, `UserSessionRepository` |
| Services | 22 | `AcademicYearService`, `AssessmentService`, `AuditService`, `AuthService`, `ClassLevelService`, `ClassService`, `CommentService`, `ExcelService`, `GuardianService`, `ImportJobService`, `LoginHistoryService`, `ReportService`, `SchoolSettingsService`, `ScoreService`, `StreamService`, `StudentImportService`, `StudentService`, `SubjectService`, `TeacherAssignmentService`, `TermService`, `TopicService`, `UserService` |
| API controllers | 20 | `AdminAcademicYearApiController`, `AdminAssessmentApiController`, `AdminAssignmentApiController`, `AdminAuditApiController`, `AdminClassApiController`, `AdminClassLevelApiController`, `AdminLoginHistoryApiController`, `AdminReportRequestApiController`, `AdminSettingsApiController`, `AdminStreamApiController`, `AdminStudentImportApiController`, `AdminSubjectApiController`, `AdminTermApiController`, `AdminTopicApiController`, `AdminUserApiController`, `AuthApiController`, `ReportApiController`, `TeacherAssessmentApiController`, `TeacherCommentApiController`, `TeacherMarkApiController` |
| Web controllers | 7 | `ErrorWebController`, `LoginWebController`, `StudentDashboardWebController`, `StudentMarksWebController`, `StudentProfileWebController`, `StudentReportsWebController`, `StudentResultsWebController` |
| Request DTOs | 21 | `CreateAcademicYearRequest`, `CreateAssessmentRequest`, `CreateAssignmentRequest`, `CreateClassLevelRequest`, `CreateClassRequest`, `CreateGuardianRequest`, `CreateStreamRequest`, `CreateStudentRequest`, `CreateSubjectRequest`, `CreateTermRequest`, `CreateTopicRequest`, `CreateUserRequest`, `GenerateReportRequest`, `LoginRequest`, `SaveCommentRequest`, `SaveMarksBatchRequest`, `SaveMarksRequest`, `UpdateAssessmentRequest`, `UpdateSchoolSettingsRequest`, `UpdateStudentRequest`, `UpdateUserRequest` |
| Response DTOs | 28 | `AcademicYearResponse`, `ApiResponse`, `AssessmentResponse`, `AssignmentResponse`, `AuditLogResponse`, `ClassLevelResponse`, `ClassResponse`, `CommentResponse`, `ErrorResponse`, `GuardianResponse`, `ImportConfirmResponse`, `ImportJobResponse`, `ImportPreviewResponse`, `LoginHistoryResponse`, `LoginResponse`, `MarkGridResponse`, `PageResult`, `ReportResponse`, `ResetPasswordResponse`, `SchoolSettingsResponse`, `ScoreResponse`, `StreamResponse`, `StudentResponse`, `SubjectResponse`, `TermResponse`, `TopicResponse`, `UserResponse`, `UserSummary` |
| Mappers | 17 | `AcademicYearMapper`, `AssessmentMapper`, `AuditLogMapper`, `ClassLevelMapper`, `ClassMapper`, `CommentMapper`, `ImportJobMapper`, `LoginHistoryMapper`, `SchoolSettingsMapper`, `ScoreMapper`, `StreamMapper`, `StudentMapper`, `SubjectMapper`, `TeacherAssignmentMapper`, `TermMapper`, `TopicMapper`, `UserMapper` |
| Exception package Java files | 6 | `AuthenticationException`, `ErrorCode`, `GlobalExceptionHandler`, `PermissionDeniedException`, `ResourceNotFoundException`, `ValidationException` |

Evidence for package counts: `backend/src/main/java/com/musomi/manager/{entity,repository,service,controller,dto,mapper,exception}`. The package structure is confirmed by the listed source file paths; source code compiles as 178 backend main Java files.

## Tests

28 tracked test source files: 24 backend under `backend/src/test/java` and 4 desktop under `desktop/src/test/java`. Per-class test method counts and run outcomes are in [07-tests-audit.md](07-tests-audit.md).

## Whole-project Java totals

- **302 tracked `.java` files; 21,055 physical source lines.**
- Application source: 178 backend main + 95 desktop main = 273 Java files.
- Test sources: 24 backend + 4 desktop = 28 Java files.
- The remaining tracked Java file is `desktop/.mvn/.mvn/wrapper/MavenWrapperDownloader.java`.

No issues found for inventory accounting. The list is derived from tracked paths; generated Maven `target` output is not counted.
