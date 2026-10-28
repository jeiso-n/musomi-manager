package com.musomi.manager.controller.api;

import java.util.List;

import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.LoginHistoryResponse;
import com.musomi.manager.service.LoginHistoryService;
import com.musomi.manager.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for login history. */
@RestController
@RequestMapping("/api/v1/admin/login-history")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoginHistoryApiController {

    private final LoginHistoryService loginHistoryService;

    /** Lists school login history or filters it to a specific user. */
    @GetMapping
    public ApiResponse<List<LoginHistoryResponse>> listLoginHistory(
            @RequestParam(required = false) Long userId) {
        List<LoginHistoryResponse> history = userId == null
                ? loginHistoryService.listBySchool(CurrentUser.getSchoolId())
                : loginHistoryService.listByUser(userId);
        return ApiResponse.success(history);
    }
}
