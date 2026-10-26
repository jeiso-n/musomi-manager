# Architecture Wiring Audit

## Sidebar Navigation

| UI Element | FXML File | onAction Method | Controller Logic | Desktop API Called | Backend Endpoint |
|---|---|---|---|---|---|
| Admin Dashboard | `desktop/src/main/resources/fxml/main-layout.fxml` | `#showDashboard` | Loads `/fxml/dashboard.fxml`. | None | None |
| Users | `main-layout.fxml` | `#showUsers` | Loads `/fxml/admin/users.fxml`. | None | None |
| Classes | `main-layout.fxml` | `#showClasses` | Loads `/fxml/admin/classes.fxml`. | None | None |
| Subjects | `main-layout.fxml` | `#showSubjects` | Loads `/fxml/admin/subjects.fxml`. | None | None |
| Topics | `main-layout.fxml` | `#showTopics` | Loads `/fxml/admin/topics.fxml`. | None | None |
| Students | `main-layout.fxml` | `#showStudents` | Loads `/fxml/admin/students.fxml`. | None | None |
| Teacher Assignments | `main-layout.fxml` | `#showTeacherAssignments` | Loads `/fxml/admin/teacher-assignments.fxml`. | None | None |
| Audit Log | `main-layout.fxml` | `#showAuditLog` | Loads `/fxml/admin/audit-log.fxml`. | None | None |

The Admin Dashboard button is present in the current snapshot above Users. The earlier missing-button finding describes a previous file state.

## Admin Users

| UI Element | FXML File | onAction Method | Controller Logic | Desktop API Called | Backend Endpoint |
|---|---|---|---|---|---|
| Add User | `admin/users.fxml` | `#showAddUserDialog` | Opens the modal, waits, then reloads if the dialog reports creation. | `AdminUserApi.createUser()` through `TaskRunner`. | `POST /api/v1/admin/users` |
| Create User | `admin/add-user.fxml` | `#handleCreateUser` | Validates fields, builds the request, posts it asynchronously, shows a toast, marks creation successful, and closes. | `POST /admin/users`. | `AdminUserApiController.createUser()`. |
| Deactivate User | Generated table button | Programmatic `handleDeactivateUser()` | Confirms, then deletes asynchronously and reloads. | `DELETE /admin/users/{id}`. | `DELETE /api/v1/admin/users/{id}` |

The create endpoint can return 400 when validation fails: blank/oversized username, password shorter than eight characters, blank full name, null role, or a role that cannot deserialize to the backend enum.

## Admin Audit Log

| UI Element | FXML File | onAction Method | Controller Logic | Desktop API Called | Backend Endpoint |
|---|---|---|---|---|---|
| Audit Log page | `admin/audit-log.fxml` | None | Loaded by `MainLayoutController.showAuditLog()`. | None directly | None |
| Audit table initialization | `admin/audit-log.fxml` | FXMLLoader `initialize()` | Configures columns, search filtering, and loads backend data. | `AdminAuditApi.getAuditLogs()` via `TaskRunner`. | `GET /api/v1/admin/audit` |
| Audit log loading | `AuditLogController.loadLogsFromBackend()` | None | Maps responses to `AuditRow` and populates the table. | `GET /admin/audit`. | `AdminAuditApiController.listAudit()` |

Hardcoded sample audit data is not used by the current controller.

## Admin Teacher Assignments

| UI Element | FXML File | onAction Method | Controller Logic | Desktop API Called | Backend Endpoint |
|---|---|---|---|---|---|
| Assign Teacher | `admin/teacher-assignments.fxml` | `#assignTeacher` | Validates selections, builds a request, and posts asynchronously. | `POST /admin/assignments`. | `POST /api/v1/admin/assignments` |
| Clear | `teacher-assignments.fxml` | `#clearForm` | Clears selections and resets the message. | None | None |
| Refresh | `teacher-assignments.fxml` | `#refreshAssignments` | Reloads assignments. | `GET /admin/assignments`. | `GET /api/v1/admin/assignments` |
| Remove Assignment | `teacher-assignments.fxml` | `#removeAssignment` | Confirms and deletes the selected assignment. | `DELETE /admin/assignments/{id}`. | `DELETE /api/v1/admin/assignments/{id}` |
| Teacher dropdown | Controller initialization | None | Fetches teachers, classes, and subjects asynchronously. | `GET /teacher/teachers`, `GET /admin/classes`, `GET /admin/subjects`. | Corresponding `/api/v1` endpoints |

The original 403 occurred because assignment list/create/delete routes are Admin-only. Teacher and subject reference reads are separate routes with teacher-read access.

## Teacher Assessments

| UI Element | FXML File | onAction Method | Controller Logic | Desktop API Called | Backend Endpoint |
|---|---|---|---|---|---|
| Create Assessment | `teacher/assessments.fxml` | `#handleCreateAssessment` | Opens the create form; does not submit. | None | None |
| Save Draft | `teacher/assessments.fxml` | `#handleSaveDraft` | Validates fields, builds `CreateAssessmentRequest`, and creates asynchronously. | `TeacherAssessmentApi.createAssessment()`. | `POST /api/v1/teacher/assessments` |
| Save Changes | `teacher/assessments.fxml` | `#handleSaveDraft` | Builds an update request and sends it directly through `ApiClient`. | `PUT /teacher/assessments/{id}`. | `PUT /api/v1/teacher/assessments/{id}` |
| Publish Assessment | `teacher/assessments.fxml` | `#handlePublish` | Confirms publication, then posts to the publish route. | `POST /teacher/assessments/{id}/publish`. | `POST /api/v1/teacher/assessments/{id}/publish` |

The create flow submits only when Save Draft is pressed; the initial Create Assessment button opens the form.
