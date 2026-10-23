# 9. Error codes and error envelopes

## ErrorCode coverage

`backend/src/main/java/com/musomi/manager/exception/ErrorCode.java` declares 38 enum values. Every enum value has a corresponding entry in `docs/shared/ERROR_CODES.md`.

Thirteen enum values have no production throw/reference outside enum declarations:

`ADMIN_ONLY`, `ASSESSMENT_NOT_DRAFT`, `FEEDBACK_TOO_LONG`, `FILE_TOO_LARGE`, `IMPORT_DUPLICATE_ROWS`, `IMPORT_VALIDATION_FAILED`, `NOT_YOUR_SUBJECT`, `NO_MARKS_PUBLISHED`, `SCORE_REQUIRED`, `STUDENT_INACTIVE`, `STUDENT_NOT_ENROLLED`, `UNSUPPORTED_FILE_TYPE`, `WEAK_PASSWORD`.

**Evidence:** `backend/src/main/java/com/musomi/manager/exception/ErrorCode.java` declares, for example, `WEAK_PASSWORD(HttpStatus.BAD_REQUEST, ...)`; `docs/shared/ERROR_CODES.md` has a `### WEAK_PASSWORD` heading, but no production usage was found for the listed values. **Should be:** use codes for their documented scenarios, or remove unsupported codes from the public contract once confirmed unnecessary.

The API contract's canonical error-code list omits two declared codes: `FILE_TOO_LARGE` and `UNSUPPORTED_FILE_TYPE`. **Evidence:** `docs/shared/API_CONTRACT.md` §3 lists the client error codes, while `backend/src/main/java/com/musomi/manager/exception/ErrorCode.java` declares both values. **Should be:** include them in the API contract or remove them if not supported.

## Response envelope

Standard handled API exceptions use `ApiResponse.error(...)`. `GlobalExceptionHandler` returns that form for resource-not-found, validation, permission, authentication, request-validation, and generic exceptions.

**Evidence:** `backend/src/main/java/com/musomi/manager/exception/GlobalExceptionHandler.java` uses `.body(ApiResponse.error(ErrorCode.INTERNAL_ERROR))` in `handleGenericException`; `backend/src/main/java/com/musomi/manager/dto/response/ApiResponse.java` exposes `error(ErrorCode errorCode)`.

No nonstandard error body from the global exception handler was found. `SecurityConfig` writes the session-expired JSON directly at the authentication entry point rather than constructing `ApiResponse`; it contains the contract's `error` object but does not use the `ApiResponse` implementation. **Evidence:** `backend/src/main/java/com/musomi/manager/config/SecurityConfig.java` writes `"{\"error\":{\"code\":\"SESSION_EXPIRED\",...}}";`. The wire shape appears consistent with the documented error object, but exact parity for optional `details` and serialization is not covered by a test; **unknown**.

## Raw RuntimeException

No `new RuntimeException(...)` or `throw new RuntimeException(...)` was found in backend production Java sources. No issues found for raw `RuntimeException` use.
