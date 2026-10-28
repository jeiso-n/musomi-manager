# 8. Security audit

Static review only; no penetration test or production deployment review was performed. Findings below distinguish development/demo artifacts from confirmed production exposure.

## Secrets and credentials

| Finding | Evidence | Expected state |
|---|---|---|
| Development JWT fallback is embedded in default configuration. | `backend/src/main/resources/application.properties`: `jwt.secret=${JWT_SECRET:musomi-manager-dev-secret-change-in-production-min-32-chars-long}` | Require an externally provided high-entropy secret in every profile that can serve real users. |
| Dev seeder creates a known default password and logs it. | `backend/src/main/java/com/musomi/manager/config/DevDataSeeder.java`: `.passwordHash(passwordEncoder.encode("password123"))` and `log.info("Seeded dev admin user: admin / password123");` | Never log credentials; use a one-time secret or force a password change. It is `@Profile("dev")`, so production activation depends on runtime profile configuration. |
| Desktop mock responses contain a fake token literal. | `desktop/src/main/java/com/musomi/desktop/util/MockData.java`: `"mock-token-abc123"` | Keep mock credentials unmistakably isolated from live authentication; no evidence indicates this is a real token. |

`backend/src/main/resources/application-prod.properties` instead states `jwt.secret=${JWT_SECRET}` and explicitly says the variable has no default.

## Endpoint authorization and identity

API controllers have role guards except authentication routes; auth routes are intentionally public where required for login. No sensitive API endpoint with explicit `permitAll` was found. However, the global web rule grants any authenticated role access to every `/student/**` route without requiring `STUDENT`.

**Evidence:** `backend/src/main/java/com/musomi/manager/config/SecurityConfig.java` contains `.requestMatchers("/api/v1/**", "/student/**").authenticated()`, and the only explicit public endpoint pattern includes `"/api/v1/auth/login"`. **Should be:** constrain student routes to the STUDENT role and resolve/authorize the student's own identity for every data operation.

Many controller methods supply fixed identity/tenant constants despite authenticated callers. **Evidence:** `backend/src/main/java/com/musomi/manager/controller/api/TeacherMarkApiController.java` declares `private static final Long TEACHER_ID = 1L;`; `backend/src/main/java/com/musomi/manager/controller/api/AdminStudentImportApiController.java` declares `SCHOOL_ID = 1L` and `USER_ID = 1L`. **Should be:** derive actor and school from the authenticated principal and enforce tenant/ownership boundaries. This is a cross-tenant/actor authorization risk, not just test scaffolding.

## Logging and console output

- No backend log statement containing a password or token was found. The dev seeder does log the default password, noted above.
- Desktop source contains `System.out.println` statements. Example: `desktop/src/main/java/com/musomi/desktop/controller/teacher/MyClassesController.java` says `System.out.println("FAILED TO LOAD class-roster.fxml: " + ex.getMessage());`. `desktop/src/main/java/com/musomi/desktop/controller/MainLayoutController.java` also prints navigation diagnostics. **Should be:** use the project logger and avoid printing exception details directly to standard output.

## CORS and Actuator

- No explicit CORS configuration was found. Whether an external proxy or deployment layer supplies CORS policy is **unknown**; the resulting browser-origin policy cannot be established from this source review.
- Backend POM includes Actuator; `SecurityConfig` explicitly permits `/actuator/health`. No custom actuator exposure configuration was found. Health is the only explicitly identified endpoint; any additional exposed actuator endpoint is **unknown** without the effective Spring configuration/runtime environment. **Should be:** explicitly configure and test exposure and access control, with only operationally required endpoints enabled.

## Overall

The default profile's development JWT fallback, dev password logging, fixed actor IDs, and missing STUDENT role restriction should be addressed before a real school deployment. No issues found for other explicit `permitAll` route patterns in the inspected security configuration.
