package com.musomi.desktop.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.model.PageResult;
import com.musomi.desktop.model.StudentResponse;

@RequiredArgsConstructor
@Slf4j
public class AdminStudentApi {

    private final ApiClient client = ApiClient.getInstance();

    public PageResult<StudentResponse> getStudents(int page, int size, String search) {
        String encodedSearch = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        String path = "/admin/students?page=" + page + "&size=" + size
                + "&search=" + encodedSearch;
        log.debug("Fetching admin students from {}", path);

        StudentResponse[] students = client.get(path, StudentResponse[].class);
        List<StudentResponse> data = students == null ? List.of() : Arrays.asList(students);
        return new PageResult<>(data, page, size, data.size(), data.isEmpty() ? 0 : 1);
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
                                       Long currentClassId, Long currentStreamId, String status) {}
}
