# Musomi Manager - Current Status
_Date: 2026-10-23_

## 1. Executive Summary
- **Estimated v1 completion: about 45%.** This is a code-based estimate, not a project-tracking metric: core backend services and teacher assessment APIs exist, but major user workflows still stop at mock data or only partially use persisted records.
- **Top 3 blockers:**
  1. The student Web Portal is mostly demo content, and its password-change handler does not verify or change a password.
  2. The Desktop Admin Users, Classes, and Subjects screens use sample data; Teacher Assignments is an empty controller.
  3. Teacher Dashboard and Reports still depend on `AssessmentStore`; new assessment creation is not wired from the Desktop form to the API.

## 2. Backend
- **Status:** Core API domains are implemented, but contract coverage is not complete for v1.
- **Counts:** 25 JPA entity source files; 26 SQL migration files (V001–V025 plus V099 test data); 29 controller source files (22 API and 7 web); 27 Java test source files.
- **Tests:** `mvn test` completed with **150 tests, 0 failures, 0 errors, and 1 skipped** (149 executed successfully).
- **API contract implementation:** Controller mappings exist for authentication, admin configuration and management, teacher classes/assessments/marks/comments, and reports. The student API section of `API_CONTRACT.md` documents student dashboard, marks, subject, results, reports, profile, password, and `/student/me` endpoints; no corresponding student REST API controller mappings were found. Some paths exist as Web routes, but their data is static/demo as detailed below.
- **Missing/Blockers:**
  - Several documented student endpoints have no REST API implementation.
  - The backend test suite has one skipped test; API coverage is not equivalent to full end-to-end verification of all documented endpoints.
  - The code audit found functional backend APIs but does not establish a fully data-backed student portal or deployed pilot configuration.

## 3. Web Portal
- **Status:** Partially implemented; most student pages are demo-backed rather than database-backed.
- **Real features:** Student Dashboard can resolve a student and load published scores from repositories when a matching user/student exists. Login and error pages are present; the login controller presents the form and maps authentication error codes.
- **Mocked/Missing features:**
  - **Dashboard:** Demo identity, recent marks, and summary fallback remain. Term/year are fixed; class position and class size are hardcoded even when scores are loaded.
  - **Marks and subject detail:** `StudentMarksWebController` injects repositories but builds lists and subject details from hardcoded `List.of`/`Map.of`.
  - **Results:** Subjects, averages, grades, term, year, and rank are hardcoded.
  - **Reports:** Report cards list and PDF student, term, settings, and subject data are hardcoded. PDF generation uses `PdfService`, but the report content is not loaded for the logged-in student.
  - **Profile/password:** Student and guardian profile details are hardcoded. The password handler checks only whether the new values match and returns success markup; it does not authenticate the current password or persist a password change.
  - School name and some other display metadata are hardcoded in the Web controllers.

## 4. Desktop - Admin
- **Status:** Mock-only for the audited Users, Classes, Subjects, and Teacher Assignments screens.
- **Real features (API-connected):** None of these four controllers call `ApiClient`.
- **Mocked/Missing features:**
  - **Users:** Sample rows are loaded in memory; add/edit/deactivate changes are local.
  - **Classes:** Sample rows are loaded in memory; add/edit/deactivate changes are local.
  - **Subjects:** Sample rows are loaded in memory; add/edit/deactivate changes are local.
  - **Teacher Assignments:** `TeacherAssignmentController` is an empty class (stub).

## 5. Desktop - Teacher
- **Status:** Mixed: classes, roster, assessment listing/review/publish, and mark entry use backend APIs; dashboard and reports remain local. Assessment creation is not fully wired.
- **Real features (API-connected):**
  - **My Classes:** Fetches `/teacher/classes` through `ApiClient` and `TaskRunner`.
  - **Class Roster:** Resolves class/stream identifiers from the teacher classes API when needed, then fetches the student roster from the backend.
  - **Assessments:** Fetches `/teacher/assessments`, loads marks grids for review/view, updates existing draft assessments, and publishes through the publish endpoint.
  - **Mark Entry:** Fetches assessments and each selected assessment grid, maps server students to scores, and saves committed marks in batches through the API.
- **Mocked/Missing features:**
  - **Dashboard:** Summary values and pending assessment navigation still use `AssessmentStore`.
  - **Reports:** Report data is computed from `AssessmentStore`, not the backend.
  - **Assessment creation:** The current form does not collect the backend class, subject, term, and topic IDs required by the create endpoint; it reports that creation is unavailable. Existing draft edits also cannot change class/subject/topics through this form.
  - The legacy `AssessmentStore` and its local persistence remain in the project for other screens, including Dashboard and Reports.

## 6. Immediate Next Steps (Action Plan)
1. **Make the Web Portal safe and data-backed:** bind all student pages and report downloads to the authenticated student and repositories/services; remove demo fallbacks; implement password verification and persistence.
2. **Wire Desktop Admin management:** connect Users, Classes, and Subjects list/create/update/deactivate actions to their existing admin APIs; implement Teacher Assignments using the assignments endpoints.
3. **Complete the Desktop teacher workflow:** add backend-ID selection for class, stream, subject, term, and topics to the assessment form, then wire assessment creation; replace Dashboard and Reports `AssessmentStore` reads with API-backed data.
4. **Close API contract gaps:** implement or explicitly remove unsupported student REST endpoint contracts, including `/student/me`, and verify every documented route against controller mappings.
5. **Validate pilot workflows end to end:** add/run integration coverage for admin assignments and CRUD, teacher class-to-roster-to-marks-to-publish flow, and student login-to-results/report flow; resolve the skipped test or document why it must remain skipped.
