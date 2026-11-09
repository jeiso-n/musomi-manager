package com.musomi.manager.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long studentId,
        String studentName,
        Long termId,
        String termName,
        Integer academicYear,
        Long fileSizeBytes,
        LocalDateTime generatedAt,
        String downloadUrl,
        Long classId,
        Long streamId,
        String reportStatus,
        Integer expectedAssessmentCount,
        Integer recordedAssessmentCount,
        BigDecimal overallAverage,
        String overallGrade,
        Integer position,
        Integer rankingCohortSize
) {
}
