package com.musomi.manager.controller.api;

import com.musomi.manager.dto.request.UpdateSchoolSettingsRequest;
import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.SchoolSettingsResponse;
import com.musomi.manager.service.SchoolSettingsService;
import com.musomi.manager.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for school settings. */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsApiController {

    private final SchoolSettingsService schoolSettingsService;

    /** Returns the current school's settings. */
    @GetMapping
    public ApiResponse<SchoolSettingsResponse> getSettings() {
        return ApiResponse.success(schoolSettingsService.getSettings(CurrentUser.getSchoolId()));
    }

    /** Updates the current school's settings. */
    @PutMapping
    public ApiResponse<SchoolSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateSchoolSettingsRequest request) {
        return ApiResponse.success(schoolSettingsService.updateSettings(CurrentUser.getSchoolId(), request));
    }
}
