package com.musomi.desktop.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.PageResult;
import com.musomi.desktop.model.StudentResponse;
import com.musomi.desktop.model.StudentAccountCredentialResponse;
import com.musomi.desktop.model.StudentAccountProvisioningResponse;

@RequiredArgsConstructor
@Slf4j
public class AdminStudentApi {

    public static final List<String> VALID_STATUSES = List.of(
            "ACTIVE",
            "INACTIVE",
            "TRANSFERRED",
            "GRADUATED",
            "DROPPED_OUT",
            "SUSPENDED"
    );

    private final ApiClient client = ApiClient.getInstance();

    /**
     * Normalizes a student status string (e.g. {@code "Active"} or {@code "Dropped Out"})
     * to the uppercase database/API format ({@code "ACTIVE"}, {@code "DROPPED_OUT"}),
     * and validates that the normalized value is in {@link #VALID_STATUSES}.
     *
     * @throws IllegalArgumentException if a non-blank status is not in {@link #VALID_STATUSES}
     */
    public static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String trimmed = status.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("\\s+", "_");
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported student status: \"" + status
                    + "\". Allowed values: " + VALID_STATUSES);
        }
        return normalized;
    }

    public PageResult<StudentResponse> getStudents(int page, int size, String search) {
        String encodedSearch = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        String path = "/admin/students?page=" + page + "&size=" + size
                + "&search=" + encodedSearch;
        log.debug("Fetching admin students from {}", path);

        StudentResponse[] students = client.get(path, StudentResponse[].class);
        List<StudentResponse> data = students == null ? List.of() : Arrays.asList(students);
        return new PageResult<>(data, page, size, data.size(), data.isEmpty() ? 0 : 1);
    }

    public StudentAccountProvisioningResponse provisionStudentAccounts() {
        return client.post("/admin/students/provision-accounts", java.util.Map.of(),
                StudentAccountProvisioningResponse.class);
    }

    public StudentAccountCredentialResponse resetStudentPassword(Long studentId) {
        return client.post("/admin/students/" + studentId + "/reset-password",
                java.util.Map.of(), StudentAccountCredentialResponse.class);
    }

    public void deactivateStudent(Long studentId) {
        client.delete("/admin/students/" + studentId);
    }

    public StudentResponse createStudent(String admissionNumber, String fullName, Long currentClassId) {
        return createStudent(new CreateStudentRequest(admissionNumber, fullName, null, null,
                currentClassId, null, null));
    }

    public StudentResponse createStudent(CreateStudentRequest request) {
        return client.post("/admin/students",
                request,
                StudentResponse.class);
    }

    public StudentResponse updateStudent(Long id, String fullName, Long currentClassId,
                                        String status) {
        return updateStudent(id, new UpdateStudentRequest(
                fullName, null, null, currentClassId, null, status));
    }

    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        return client.put("/admin/students/" + id,
                request,
                StudentResponse.class);
    }

    public record CreateStudentRequest(
            String admissionNumber,
            String fullName,
            String gender,
            Object dateOfBirth,
            Long currentClassId,
            Long currentStreamId,
            Object guardian) {
    }

    public record UpdateStudentRequest(String fullName, String gender, Object dateOfBirth,
                                       Long currentClassId, Long currentStreamId, String status) {
        public UpdateStudentRequest {
            status = normalizeStatus(status);
        }
    }
}
