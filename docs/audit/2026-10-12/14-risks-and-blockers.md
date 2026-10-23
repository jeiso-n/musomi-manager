# 14. Blockers and risks

## What prevents a pilot school from using this today?

The codebase alone does not establish a production-ready, end-to-end school workflow. The directly evidenced blockers are:

1. **Reports cannot be generated or downloaded.** `backend/src/main/java/com/musomi/manager/controller/api/ReportApiController.java`: `throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Report generation is not available yet")`. The v1 scope includes PDF report cards.
2. **Student screens display fabricated records.** `backend/src/main/java/com/musomi/manager/controller/web/StudentDashboardWebController.java`: `model.addAttribute("firstName", "Sarah")`; the controller similarly hardcodes marks. **Expected:** authenticated student's persisted data only.
3. **Several controllers use fixed school/user/teacher identifiers.** `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java`: `private static final Long TEACHER_ID = 1L;`. **Expected:** derive principal and tenant identifiers from authenticated request context.
4. **Backend full test run is not green in this environment.** `backend/target/surefire-reports/com.musomi.manager.MusomiManagerApplicationTests.txt`: `Connection to localhost:5432 refused`. **Expected:** reproducible database-backed verification before release.
5. **Required deployment prerequisites are unknown.** No deployment manifests, production environment, school acceptance, backup/restore evidence, or operational runbook was verified in this audit. Actual availability/status is **unknown**, not asserted absent.

## Highest-risk area

**Authorization and tenant isolation.** Controllers pass constant actor and school IDs while security verifies authentication/role at route level; user-specific data access could therefore run under the wrong identity or tenant. `backend/src/main/java/com/musomi/manager/controller/api/AdminStudentImportApiController.java` contains `private static final Long SCHOOL_ID = 1L;` and `private static final Long USER_ID = 1L;`. **Expected:** derive and validate principal, role, school membership, and target ownership at the service boundary.

## Most likely to break first

**Database-backed application startup/integration.** The full context test errors while trying to reach PostgreSQL, so migrations, ORM validation, and full route wiring have not been proven by the local full test run. `backend/src/test/java/com/musomi/manager/MusomiManagerApplicationTests.java` is a `@SpringBootTest`; its report says PostgreSQL connection was refused. **Expected:** run the application-context test against a reproducible PostgreSQL instance and keep it passing in CI.

Also at high functional risk: PDF report generation, since the controller explicitly returns 501; and any operation using the fixed actor/tenant constants.
