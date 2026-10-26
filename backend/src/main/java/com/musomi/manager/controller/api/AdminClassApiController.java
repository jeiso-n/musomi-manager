package com.musomi.manager.controller.api;

import java.util.List;

import com.musomi.manager.dto.request.CreateClassRequest;
import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.ClassResponse;
import com.musomi.manager.service.ClassService;
import com.musomi.manager.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for classes. */
@RestController
@RequestMapping("/api/v1/admin/classes")
@RequiredArgsConstructor
public class AdminClassApiController {

    private final ClassService classService;

    /** Lists classes, optionally filtered by academic year. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassResponse>> listClasses(
            @RequestParam(required = false) Long yearId) {
        return ApiResponse.success(classService.listClasses(CurrentUser.getSchoolId(), yearId));
    }

    /** Creates a class. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @Valid @RequestBody CreateClassRequest request) {
        ClassResponse created = classService.createClass(CurrentUser.getSchoolId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassResponse> updateClass(
            @PathVariable Long id, @Valid @RequestBody CreateClassRequest request) {
        return ApiResponse.success(classService.updateClass(
                id, CurrentUser.getSchoolId(), request));
    }
}
