# JavaFX Desktop Integration Status

| Feature | Controller File | Status | Evidence |
|---|---|---|---|
| Login | `LoginController.java` | REAL | Calls `AuthService.login()`; `AuthService` delegates to `AuthApi.login()`, which sends `POST /auth/login` through `ApiClient`. |
| Dashboard | `DashboardController.java` | MOCK | Summary counts and pending-assessment navigation use `AssessmentStore` (`getPendingAssessments()`, `countDistinctClasses()`, `mockRosterSize()`, `countPublished()`). The greeting uses the authenticated session profile, but dashboard feature data is local. |
| My Classes | `MyClassesController.java` | MOCK | Renders the hardcoded `MOCK_CLASSES` `List.of(...)`; no backend call. |
| Class Roster | `ClassRosterController.java` | MOCK | Loads hardcoded `MOCK_STUDENTS` `List.of(...)` in `setClassData()`; no backend call. |
| Assessments List | `AssessmentsController.java` | MOCK | Lists and changes assessments through `AssessmentStore`; class/subject/topic options are hardcoded `List.of(...)`. Create/edit and publish call `AssessmentStore.saveDefault()`, which persists locally via `AssessmentRepository`, not the backend. |
| Mark Entry | `MarkEntryController.java` | MOCK | Loads assessments and marks from `AssessmentStore`; committing a mark calls `AssessmentStore.saveDefault()`. The UI explicitly reports that server sync is unavailable. |
| Reports | `ReportPreviewController.java` | MOCK | Builds class options and report rows from `AssessmentStore.getAssessments()` and `AssessmentStore.getMarks()`; no backend call. |
| Admin User Management | `UsersController.java` | MOCK | Populates the table with three hardcoded sample users; add/edit/deactivate operations update the in-memory `ObservableList`/`UserRow` only. |
| Admin Teacher Assignments | `TeacherAssignmentController.java` | STUB | Controller file contains only an empty class; no feature logic or backend integration. |

**Summary:** Login is the only audited feature wired to the Spring Boot backend. Assessment and mark data are seeded/local and may be persisted to `~/.musomi/assessments.json` by `AssessmentRepository`; the other audited features use hardcoded or in-memory mock data, or are an empty stub.
