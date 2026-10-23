# 13. v1 scope progress

This is an estimate of code-level implementation, not a pilot acceptance result. “Partial” means relevant code exists but a documented journey is incomplete, uses fixed/mock data, or lacks verified end-to-end behavior.

| v1 item from `docs/master/MASTER.md` §4 | Status | Evidence |
|---|---|---|
| Login (ADMIN, TEACHER, STUDENT roles) | Partial | `backend/src/main/java/com/musomi/manager/controller/api/AuthApiController.java` has `@PostMapping("/login")`; no change-password API is present, and controller identity context remains fixed in other flows. |
| School structure (years, terms, classes, streams, subjects, topics) | Partial | CRUD/list API controllers exist; `backend/src/main/java/com/musomi/manager/controller/api/AdminAcademicYearApiController.java` declares `private static final Long SCHOOL_ID = 1L`. |
| Student records with Excel import | Partial | `backend/src/main/java/com/musomi/manager/controller/api/AdminStudentImportApiController.java` exposes import preview/confirm/template; student CRUD API paths from `API_CONTRACT.md` are absent. |
| Teacher assignments | Partial | `backend/src/main/java/com/musomi/manager/controller/api/AdminAssignmentApiController.java` maps GET/POST/DELETE assignments; class/stream roster endpoints documented for teachers are absent. |
| Assessment creation with topic tagging | Partial | `backend/src/main/java/com/musomi/manager/service/AssessmentService.java` contains assessment workflows; student/tenant identity and full live-route flow remain unverified. |
| Mark entry grid (keyboard-driven, auto-save) | Partial | `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java` exposes grid and score APIs; `desktop/src/main/java/com/musomi/desktop/controller/teacher/MarkEntryController.java` holds UI behavior. `TEACHER_ID` is hardcoded in the API controller and end-to-end persistence was not tested. |
| Draft/Published workflow | Partial | `backend/src/main/java/com/musomi/manager/service/AssessmentService.java` logs `"Assessment published"` and defines `publishAssessment`; controller/identity use remains incomplete for production. |
| Subject and class teacher comments | Partial | `backend/src/main/java/com/musomi/manager/controller/api/TeacherCommentApiController.java` exposes comment routes; it passes `TEACHER_ID = 1L` and `SCHOOL_ID = 1L`. |
| Term report card PDF | Not started | `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java` throws `HttpStatus.NOT_IMPLEMENTED` for generation and returns 501 for download. |
| Student portal (web) to view marks and download reports | Partial | `backend/src/main/java/com/musomi/manager/controller/web/StudentDashboardWebController.java` returns `"student/dashboard"` but hardcodes student, term and marks; reports are not downloadable because report download returns 501. |
| Audit log for key actions | Partial | `backend/src/main/java/com/musomi/manager/service/AuditService.java` defines `public void log(...)`; no production state-changing service calls it. |
| School settings (name, logo, grading scale) | Partial | `backend/src/main/java/com/musomi/manager/controller/api/AdminSettingsApiController.java` supports GET/PUT; no logo-upload API exists and request DTO covers only a subset of the documented settings. |

### Estimate

**Estimated v1 completion: 35%.** This is a weighted engineering judgment: foundational schema and service/API pieces exist, but reports, real student data, identity/tenant isolation, student CRUD, audit integration, and verified pilot journeys are incomplete. It is not a measured test-coverage percentage. No evidence of a live pilot school or accepted end-to-end release was found.

**Project criterion:** `docs/master/MASTER.md` §4 says, `Never start a later version until the previous one is stable and used by at least one real school.` No deployment or real-school use evidence was identified in the repository; actual pilot status is **unknown**.
