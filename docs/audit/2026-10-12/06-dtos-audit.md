# 6. DTO audit

## Record usage and validation

- Request DTOs: 21; all are records. 20 declare Bean Validation constraints; `UpdateSchoolSettingsRequest` has none.
- Response DTOs: 27 are records; `ApiResponse` is a class wrapper used throughout the REST API.
- No unused request/response DTO was identified in backend main Java references.

**Evidence:** `backend/src/main/java/com/musomi/manager/dto/request/UpdateSchoolSettingsRequest.java` declares `public record UpdateSchoolSettingsRequest(...)` with four unconstrained fields. **Should be:** constrain writable settings according to the permitted ranges/formats and invoke validation, or document fields that intentionally accept arbitrary input.

## Entity leakage and response shape

1. `backend/src/main/java/com/musomi/manager/controller/api/AdminReportRequestApiController.java` declares `public ApiResponse<ReportRequest> createReportRequest(...)`; this serializes a persistence entity instead of a response DTO. **Should be:** use a dedicated response record with only public fields.
2. `backend/src/main/java/com/musomi/manager/dto/response/ReportResponse.java` includes `String filePath`; report responses should expose the download handle rather than a filesystem storage path. **Should be:** omit internal path metadata from the externally serialized DTO.
3. `UpdateSchoolSettingsRequest` includes only four settings fields, while `docs/shared/API_CONTRACT.md` §10 says PUT has the same shape as the settings response. **Should be:** align request contract and DTO to the actual supported update fields.

## Remaining DTO checks

No other unused DTOs were found. No evidence was found that password hashes are included in response DTO records; `backend/src/main/java/com/musomi/manager/entity/User.java` maps `password_hash`, while `backend/src/main/java/com/musomi/manager/dto/response/UserResponse.java` is the public user response type. No issues found for password-hash DTO exposure.
