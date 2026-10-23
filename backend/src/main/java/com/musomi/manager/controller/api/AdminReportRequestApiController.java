package com.musomi.manager.controller.api;

import com.musomi.manager.dto.response.ApiResponse;
import com.musomi.manager.dto.response.ReportRequestResponse;
import com.musomi.manager.entity.ReportRequest;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.service.ReportService;
import com.musomi.manager.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints for report-generation requests. */
@RestController
@RequestMapping("/api/v1/admin/report-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportRequestApiController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    /** Creates a pending report request for a student and term. */
    @PostMapping
    public ApiResponse<ReportRequestResponse> createReportRequest(
            @RequestParam Long studentId,
            @RequestParam Long termId) {
        ReportRequest request = reportService.createReportRequest(
                CurrentUser.getSchoolId(), studentId, termId, CurrentUser.getUserId());
        return ApiResponse.success(toReportRequestResponse(request));
    }

    /** Marks a report request as generated. */
    @PostMapping("/{requestId}/mark-generated")
    public ResponseEntity<Void> markGenerated(
            @PathVariable Long requestId,
            @RequestParam Long reportId) {
        reportService.markGenerated(requestId, reportId);
        return ResponseEntity.noContent().build();
    }

    private ReportRequestResponse toReportRequestResponse(ReportRequest request) {
        String requestedByName = userRepository.findById(request.getRequestedBy().getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND))
                .getFullName();
        return new ReportRequestResponse(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getFullName(),
                request.getTerm().getId(),
                request.getTerm().getName(),
                request.getStatus(),
                request.getRequestedBy().getId(),
                requestedByName,
                request.getRequestedAt(),
                request.getProcessedAt());
    }
}
