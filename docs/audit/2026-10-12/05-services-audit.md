# 5. Repositories and services

## Repositories

There are 24 repositories. **All 24 are referenced by at least one service**; no unused repository was found.

Derived finder methods follow Spring Data property naming in the inspected declarations. The three custom JPQL areas are `UserRepository.findUsers`, `AcademicYearRepository.clearCurrentFlag`, and `TermRepository.clearCurrentFlag`. The update queries include `@Modifying` and `@Transactional`; field/property references inspected correspond to entity properties. Runtime validation remains unknown because the application context cannot connect to PostgreSQL.

**Evidence:** `backend/src/main/java/com/musomi/manager/repository/TermRepository.java` declares `@Modifying` and `@Query("UPDATE Term t SET t.isCurrent = false WHERE t.school.id = :schoolId AND t.isCurrent = true")`; this uses `Term.school` and `Term.isCurrent`.

No repository issues found in static naming and JPQL review. Successful Spring Data repository startup is unverified.

## Services

There are 22 service classes. All are annotated `@Service`; 21 use `@RequiredArgsConstructor`. `StudentImportService` uses a manually declared constructor. Most database writes use `@Transactional`, reads use `@Transactional(readOnly = true)`, and service public methods have Javadoc except one read helper.

| Finding | Evidence | Should be |
|---|---|---|
| `StudentImportService` does not use required constructor annotation. | `backend/src/main/java/com/musomi/manager/service/StudentImportService.java` has `@Service` and `@Slf4j`, then declares `public StudentImportService(...)` directly. | Apply the documented constructor-injection convention consistently, or document why the explicit constructor is required. |
| `AuthService.getUserSummary` has no Javadoc or read-only transaction. | `backend/src/main/java/com/musomi/manager/service/AuthService.java`: `public UserSummary getUserSummary(Long userId)`. | Add method Javadoc and `@Transactional(readOnly = true)` for the database read. |
| Audit writer methods do not have an encompassing transaction. | `backend/src/main/java/com/musomi/manager/service/AuditService.java`: `public void log(...)` performs `userRepository.findById(...)` then `auditLogRepository.save(...)` without `@Transactional`. | Make the event recording operation transactionally consistent. |
| The audit writer is not called when state changes. | A repository-wide production search found no call to `auditService.log(...)` outside `AuditService`; `backend/src/main/java/com/musomi/manager/controller/api/AdminUserApiController.java` still contains `// TODO(backend): audit_log when V023 lands`. | Call audit recording from each key mutating workflow and verify persisted audit events. |

The service layer has real workflow logic and service tests, but the API controllers still pass fixed actor/school IDs; see [08-security-audit.md](08-security-audit.md). That prevents treating service implementation as a verified user journey.
