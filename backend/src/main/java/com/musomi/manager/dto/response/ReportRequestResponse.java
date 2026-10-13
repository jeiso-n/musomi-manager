package com.musomi.manager.dto.response;

import java.time.LocalDateTime;

public record ReportRequestResponse(
        Long id,
        Long studentId,
        String studentName,
        Long termId,
        String termName,
        String status,
        Long requestedBy,
        String requestedByName,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
}
