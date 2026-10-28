# 7. Tests

## Test classes and source test counts

Counts below are test methods annotated in each Java source. `AbstractIntegrationTest` is a reusable base with no test methods.

| Test source | Count |
|---|---:|
| `backend/src/test/java/com/musomi/manager/AbstractIntegrationTest.java` | 0 |
| `backend/src/test/java/com/musomi/manager/MusomiManagerApplicationTests.java` | 1 |
| `backend/src/test/java/com/musomi/manager/controller/api/AdminUserApiControllerTest.java` | 12 |
| `backend/src/test/java/com/musomi/manager/security/JwtTokenProviderTest.java` | 10 |
| `backend/src/test/java/com/musomi/manager/service/AcademicYearServiceTest.java` | 5 |
| `backend/src/test/java/com/musomi/manager/service/AssessmentServiceTest.java` | 12 |
| `backend/src/test/java/com/musomi/manager/service/AuditServiceTest.java` | 7 |
| `backend/src/test/java/com/musomi/manager/service/AuthServiceTest.java` | 9 |
| `backend/src/test/java/com/musomi/manager/service/ClassLevelServiceTest.java` | 3 |
| `backend/src/test/java/com/musomi/manager/service/ClassServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/CommentServiceTest.java` | 7 |
| `backend/src/test/java/com/musomi/manager/service/ExcelServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/ImportJobServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/LoginHistoryServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/ReportServiceTest.java` | 7 |
| `backend/src/test/java/com/musomi/manager/service/SchoolSettingsServiceTest.java` | 5 |
| `backend/src/test/java/com/musomi/manager/service/ScoreServiceTest.java` | 8 |
| `backend/src/test/java/com/musomi/manager/service/StreamServiceTest.java` | 3 |
| `backend/src/test/java/com/musomi/manager/service/StudentImportServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/SubjectServiceTest.java` | 3 |
| `backend/src/test/java/com/musomi/manager/service/TeacherAssignmentServiceTest.java` | 6 |
| `backend/src/test/java/com/musomi/manager/service/TermServiceTest.java` | 4 |
| `backend/src/test/java/com/musomi/manager/service/TopicServiceTest.java` | 3 |
| `backend/src/test/java/com/musomi/manager/service/UserServiceTest.java` | 8 |
| `desktop/src/test/java/com/musomi/desktop/config/SessionTest.java` | 2 |
| `desktop/src/test/java/com/musomi/desktop/controller/teacher/AssessmentRepositoryTest.java` | 9 |
| `desktop/src/test/java/com/musomi/desktop/controller/teacher/AssessmentStoreTest.java` | 20 |
| `desktop/src/test/java/com/musomi/desktop/service/AuthServiceTest.java` | 4 |

Totals: 133 backend test methods and 35 desktop test methods (168 source test methods). There are 25 unit-test source files, 2 integration/slice-test sources (`@SpringBootTest` and `@WebMvcTest`), and one abstract integration base. No `@Disabled` tests were found.

## Coverage observed

Unit tests cover authentication/JWT, core services for academic years, classes, terms, topics, subjects, assignments, assessments, scores, comments, audit, users, settings, import, reports, and desktop session/assessment-store/auth logic.

Coverage gaps: no passing full Spring application-context/database test; only one web MVC controller test class (`AdminUserApiControllerTest`); no complete test of actual desktop-to-backend mark entry; no PDF generation/download test; no live student-portal identity/ownership flow. The report service tests validate metadata/service behavior, not a PDF generator.

**Evidence:** `backend/src/test/java/com/musomi/manager/MusomiManagerApplicationTests.java` is annotated `@SpringBootTest`; `backend/src/test/java/com/musomi/manager/controller/api/AdminUserApiControllerTest.java` is `@WebMvcTest(AdminUserApiController.class)`.

## Full test execution

| Command | Tests run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| Backend cached Maven `mvn test` | 133 | 0 | 1 | 0 | Failed application-context test |
| Desktop cached Maven `mvn test` | 35 | 0 | 0 | 0 | Passed |
| Combined | 168 | 0 | 1 | 0 | Not green |

The backend error is `MusomiManagerApplicationTests.contextLoads`: Flyway cannot connect to PostgreSQL at `localhost:5432` (`Connection refused`). This is an environment dependency failure; no assertion failures were reported. **Should be:** run the integration test with its expected PostgreSQL/Testcontainers configuration and require it to pass in the developer workflow.

**Evidence:** `backend/target/surefire-reports/com.musomi.manager.MusomiManagerApplicationTests.txt` says `Unable to obtain connection from database` and `Connection to localhost:5432 refused`; the same test report shows the failed context load.
