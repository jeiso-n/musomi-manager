package com.musomi.manager.controller.api;

import com.musomi.manager.dto.request.CreateUserRequest;
import com.musomi.manager.dto.request.UpdateUserRequest;
import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.PageResult;
import com.musomi.manager.dto.response.ResetPasswordResponse;
import com.musomi.manager.dto.response.UserResponse;
import com.musomi.manager.entity.enums.Role;
import com.musomi.manager.service.UserService;
import com.musomi.manager.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ApiResponse.success(
                    userService.listUsers(CurrentUser.getSchoolId(), role, active, search, pageable));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(
                CurrentUser.getSchoolId(), CurrentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id, CurrentUser.getSchoolId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(
                id, CurrentUser.getSchoolId(), CurrentUser.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id, CurrentUser.getSchoolId(), CurrentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.success(userService.resetPassword(
                id, CurrentUser.getSchoolId(), CurrentUser.getUserId()));
    }
}