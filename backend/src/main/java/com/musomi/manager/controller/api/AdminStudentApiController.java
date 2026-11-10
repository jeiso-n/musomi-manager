package com.musomi.manager.controller.api;

import java.util.List;

import com.musomi.manager.dto.request.CreateStudentRequest;
import com.musomi.manager.dto.request.UpdateStudentRequest;
import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.StudentAccountCredentialResponse;
import com.musomi.manager.dto.response.StudentAccountProvisioningResponse;
import com.musomi.manager.dto.response.StudentResponse;
import com.musomi.manager.service.StudentAccountService;
import com.musomi.manager.service.StudentService;
import com.musomi.manager.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for managing student records and portal accounts. */
@RestController
@RequestMapping("/api/v1/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentApiController {

    private final StudentService studentService;
    private final StudentAccountService studentAccountService;

    @GetMapping
    public ApiResponse<List<StudentResponse>> listStudents(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long streamId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ApiResponse.success(studentService.listStudents(
                CurrentUser.getSchoolId(), classId, streamId, status, search));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        StudentResponse created = studentService.createStudent(
                CurrentUser.getSchoolId(), CurrentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    /**
     * Creates and links student portal accounts for active students without one.
     * Temporary passwords are returned only in this response and are never logged.
     */
    @PostMapping("/provision-accounts")
    public ApiResponse<StudentAccountProvisioningResponse> provisionStudentAccounts() {
        return ApiResponse.success(studentAccountService.provisionForSchool(
                CurrentUser.getSchoolId(), CurrentUser.getUserId()));
    }

    /**
     * Rotates an existing student's password and returns the new temporary
     * credential once to the authorized school administrator.
     */
    @PostMapping("/{id}/reset-password")
    public ApiResponse<StudentAccountCredentialResponse> resetStudentPassword(@PathVariable Long id) {
        return ApiResponse.success(studentAccountService.resetStudentPassword(
                id, CurrentUser.getSchoolId(), CurrentUser.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getStudent(@PathVariable Long id) {
        return ApiResponse.success(studentService.getStudent(id, CurrentUser.getSchoolId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ApiResponse.success(studentService.updateStudent(
                id, CurrentUser.getSchoolId(), CurrentUser.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentService.deactivateStudent(id, CurrentUser.getSchoolId(), CurrentUser.getUserId());
        return ResponseEntity.noContent().build();
    }
}
