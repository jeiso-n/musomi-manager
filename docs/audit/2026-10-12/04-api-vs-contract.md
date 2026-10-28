# 4. Controllers vs API contract

Contract paths below are shown with `/api/v1`, which is the base URL in `docs/shared/API_CONTRACT.md` §2. Web endpoints are separate server-rendered routes.

## API routes in code

`—` means no request body DTO. The response column is the declared controller return type; statuses are derived from the response builder or route body.

| Method and path | Request DTO | Response type | Status |
|---|---|---|---:|
| GET `/api/v1/admin/academic-years` | — | `ApiResponse<List<AcademicYearResponse>>` | 200 |
| POST `/api/v1/admin/academic-years` | `CreateAcademicYearRequest` | `ApiResponse<AcademicYearResponse>` | 201 |
| PUT `/api/v1/admin/academic-years/{id}/current` | — | `ApiResponse<AcademicYearResponse>` | 200 |
| GET `/api/v1/admin/assessments` | — | `ApiResponse<List<AssessmentResponse>>` | 200 |
| GET `/api/v1/admin/assignments` | — | `ApiResponse<List<AssignmentResponse>>` | 200 |
| POST `/api/v1/admin/assignments` | `CreateAssignmentRequest` | `ApiResponse<AssignmentResponse>` | 201 |
| DELETE `/api/v1/admin/assignments/{id}` | — | `ResponseEntity<Void>` | 204 |
| GET `/api/v1/admin/audit` | — | `ApiResponse<List<AuditLogResponse>>` | 200 |
| GET `/api/v1/admin/classes` | — | `ApiResponse<List<ClassResponse>>` | 200 |
| POST `/api/v1/admin/classes` | `CreateClassRequest` | `ApiResponse<ClassResponse>` | 201 |
| GET `/api/v1/admin/class-levels` | — | `ApiResponse<List<ClassLevelResponse>>` | 200 |
| POST `/api/v1/admin/class-levels` | `CreateClassLevelRequest` | `ApiResponse<ClassLevelResponse>` | 201 |
| GET `/api/v1/admin/login-history` | — | `ApiResponse<List<LoginHistoryResponse>>` | 200 |
| POST `/api/v1/admin/report-requests` | Query parameters | `ApiResponse<ReportRequest>` (entity) | 200 |
| POST `/api/v1/admin/report-requests/{requestId}/mark-generated` | Query parameter | `ResponseEntity<Void>` | 204 |
| GET `/api/v1/admin/settings` | — | `ApiResponse<SchoolSettingsResponse>` | 200 |
| PUT `/api/v1/admin/settings` | `UpdateSchoolSettingsRequest` | `ApiResponse<SchoolSettingsResponse>` | 200 |
| GET `/api/v1/admin/streams` | Query parameter `classId` | `ApiResponse<List<StreamResponse>>` | 200 |
| POST `/api/v1/admin/streams` | `CreateStreamRequest` | `ApiResponse<StreamResponse>` | 201 |
| POST `/api/v1/admin/students/import` | Multipart `file` | `ApiResponse<ImportPreviewResponse>` | 200 |
| POST `/api/v1/admin/students/import/{importId}/confirm` | Path `importId` | `ApiResponse<ImportConfirmResponse>` | 200 |
| GET `/api/v1/admin/students/import/template` | — | `ResponseEntity<byte[]>` | 200 |
| GET `/api/v1/admin/subjects` | — | `ApiResponse<List<SubjectResponse>>` | 200 |
| POST `/api/v1/admin/subjects` | `CreateSubjectRequest` | `ApiResponse<SubjectResponse>` | 201 |
| GET `/api/v1/admin/subjects/{id}/topics` | — | `ApiResponse<List<TopicResponse>>` | 200 |
| POST `/api/v1/admin/subjects/{id}/topics` | `CreateTopicRequest` | `ApiResponse<TopicResponse>` | 201 |
| GET `/api/v1/admin/terms` | Optional query `yearId` | `ApiResponse<List<TermResponse>>` | 200 |
| POST `/api/v1/admin/terms` | `CreateTermRequest` | `ApiResponse<TermResponse>` | 201 |
| PUT `/api/v1/admin/terms/{id}/current` | — | `ApiResponse<TermResponse>` | 200 |
| GET `/api/v1/admin/topics` | Query `subjectId` | `ApiResponse<List<TopicResponse>>` | 200 |
| GET `/api/v1/admin/users` | Query filters and pageable | `ApiResponse<PageResult<UserResponse>>` | 200 |
| POST `/api/v1/admin/users` | `CreateUserRequest` | `ApiResponse<UserResponse>` | 201 |
| GET `/api/v1/admin/users/{id}` | — | `ApiResponse<UserResponse>` | 200 |
| PUT `/api/v1/admin/users/{id}` | `UpdateUserRequest` | `ApiResponse<UserResponse>` | 200 |
| DELETE `/api/v1/admin/users/{id}` | — | `ResponseEntity<Void>` | 204 |
| POST `/api/v1/admin/users/{id}/reset-password` | — | `ApiResponse<ResetPasswordResponse>` | 200 |
| POST `/api/v1/auth/login` | `LoginRequest` | `ApiResponse<LoginResponse>` | 200 |
| GET `/api/v1/auth/me` | — | `ApiResponse<UserSummary>` | 200 |
| POST `/api/v1/auth/logout` | — | `ResponseEntity<Void>` | 204 |
| POST `/api/v1/reports/generate` | `GenerateReportRequest` | `ApiResponse<List<ReportResponse>>` | 501 |
| GET `/api/v1/reports/student/{studentId}` | Optional query `termId` | `ApiResponse<List<ReportResponse>>` | 200 |
| GET `/api/v1/reports/download/{reportId}` | — | `ResponseEntity<byte[]>` | 501 |
| GET `/api/v1/teacher/assessments` | Query filters | `ApiResponse<List<AssessmentResponse>>` | 200 |
| POST `/api/v1/teacher/assessments` | `CreateAssessmentRequest` | `ApiResponse<AssessmentResponse>` | 201 |
| GET `/api/v1/teacher/assessments/{id}` | — | `ApiResponse<AssessmentResponse>` | 200 |
| PUT `/api/v1/teacher/assessments/{id}` | `UpdateAssessmentRequest` | `ApiResponse<AssessmentResponse>` | 200 |
| DELETE `/api/v1/teacher/assessments/{id}` | — | `ResponseEntity<Void>` | 204 |
| POST `/api/v1/teacher/assessments/{id}/publish` | — | `ApiResponse<AssessmentResponse>` | 200 |
| GET `/api/v1/teacher/students/{studentId}/comments` | Query `termId` | `ApiResponse<List<CommentResponse>>` | 200 |
| POST `/api/v1/teacher/comments` | `SaveCommentRequest` | `ApiResponse<CommentResponse>` | 200 |
| DELETE `/api/v1/teacher/comments/{id}` | — | `ResponseEntity<Void>` | 204 |
| GET `/api/v1/teacher/assessments/{id}/grid` | — | `ApiResponse<MarkGridResponse>` | 200 |
| PUT `/api/v1/teacher/scores/{scoreId}` | `SaveMarksRequest` | `ApiResponse<ScoreResponse>` | 200 |
| POST `/api/v1/teacher/assessments/{id}/scores/batch` | `SaveMarksBatchRequest` | `ApiResponse<List<ScoreResponse>>` | 200 |

Evidence: `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java` declares `@PutMapping("/scores/{scoreId}")` and accepts `@Valid @RequestBody SaveMarksRequest request`; `docs/shared/API_CONTRACT.md` §14 documents `PUT /teacher/scores/{scoreId}`.

## A) Endpoints in code but not documented

Four API endpoints are absent from the contract endpoint headings:

| Code path | Evidence |
|---|---|
| GET `/api/v1/admin/assessments` | `backend/src/main/java/com/musomi/manager/controller/api/AdminAssessmentApiController.java`: `@GetMapping` |
| GET `/api/v1/admin/login-history` | `backend/src/main/java/com/musomi/manager/controller/api/AdminLoginHistoryApiController.java`: `@GetMapping` |
| POST `/api/v1/admin/report-requests` | `backend/src/main/java/com/musomi/manager/controller/api/AdminReportRequestApiController.java`: `@PostMapping` |
| POST `/api/v1/admin/report-requests/{requestId}/mark-generated` | Same controller: `@PostMapping("/{requestId}/mark-generated")` |

Additional web routes not listed as contract endpoints: `/error`, `/error/404`, `/error/500`, `/error/access-denied`, `/login`, `/student/dashboard`, `/student/subjects/{subjectId}`, and `/student/profile/upload-avatar`. These may be deliberate HTML/error routes, but should be distinguished from the JSON API contract. `backend/src/main/java/com/musomi/manager/controller/web/StudentMarksWebController.java` declares `@GetMapping("/student/subjects/{subjectId}")`.

**Should be:** document intended externally supported API routes; document web routes separately where they are part of the supported UX.

## B) Documented endpoints not implemented at the documented path

- POST `/api/v1/auth/change-password`
- GET, POST, GET-by-id, PUT, DELETE `/api/v1/admin/students` (five CRUD routes)
- POST `/api/v1/admin/settings/logo`
- GET `/api/v1/teacher/classes`
- GET `/api/v1/teacher/classes/{classId}/streams/{streamId}/students`
- GET `/student/me`

Evidence: `docs/shared/API_CONTRACT.md` §5 documents `POST /auth/change-password`; `backend/src/main/java/com/musomi/manager/controller/api/AuthApiController.java` has mappings for login, me, and logout only. `backend/src/main/java/com/musomi/manager/controller/api/AdminStudentImportApiController.java` implements import preview/confirm/template, but is not student CRUD.

**Should be:** implement these routes or remove/mark the unsupported operations in the API contract. A separate HTML route `POST /student/profile/change-password` exists, but does not implement the documented API operation.

## C) Shape, status, and behavior differences

1. **Reports generation and download are declared but nonfunctional.** Contract §16 expects generation `200` with report data and a PDF `200`; `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java` throws `HttpStatus.NOT_IMPLEMENTED` and returns `HttpStatus.NOT_IMPLEMENTED`. **Should be:** implement the promised response or accurately mark the endpoints unavailable.
2. **Report query parameter is ignored.** `ReportApiController.listByStudent` accepts optional `termId`, but calls `reportService.listByStudent(studentId)` without using it. **Should be:** filter by the documented term or remove the parameter from the contract and route.
3. **Batch score response differs.** Contract §14 describes an object with `saved` and `scores`; `TeacherMarkApiController.saveBatch` returns `ApiResponse<List<ScoreResponse>>`. **Should be:** return the documented saved count and result list, or update the contract.
4. **Publish response differs.** Contract §13 documents a reduced object with `id`, `status`, `publishedAt`, and `affectedStudents`; code returns full `AssessmentResponse`, which does not declare `affectedStudents`. `backend/src/main/java/com/musomi/manager/dto/response/AssessmentResponse.java` declares `Long studentCount` and `Long enteredCount` instead. **Should be:** reconcile the response shape and count semantics.
5. **School settings update request is incomplete relative to its contract.** Contract §10 says PUT uses the full settings response shape; `backend/src/main/java/com/musomi/manager/dto/request/UpdateSchoolSettingsRequest.java` contains only `gradingScale`, `reportHeader`, `reportFooter`, and `currentTermId`, not school name/logo/contact or academic-year fields. **Should be:** define which fields are writable and align both schema and DTO.
6. **Report response exposes additional internal file metadata.** `backend/src/main/java/com/musomi/manager/dto/response/ReportResponse.java` contains `String filePath` and `Long fileSizeBytes`; the documented report list does not include those fields. **Should be:** return only the intended public contract, especially not the storage path.
7. **Student-page endpoint response is HTML, not the JSON example in the contract, and data is mock.** For example, `backend/src/main/java/com/musomi/manager/controller/web/StudentMarksWebController.java` returns `String` view `"student/marks"`; the contract illustrates a JSON `{ "data": [...] }` response. **Should be:** clearly specify HTML routes as server-rendered pages and connect them to actual student data, or implement the JSON endpoints separately.
8. **Raw JPA entity response.** `AdminReportRequestApiController.createReportRequest` returns `ApiResponse<ReportRequest>`, not a response DTO. **Should be:** map to a deliberately shaped response DTO.
9. **No other status mismatch was identified for mapped CRUD endpoints.** Creation methods use 201; deletes use 204; list/read/update methods use 200. Runtime response behavior could not be exercised for database-backed endpoints in this environment.

Contract duplication: `docs/shared/API_CONTRACT.md` contains two `POST /admin/streams` headings. Keep a single authoritative definition.
