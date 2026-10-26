package com.musomi.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateAssessmentRequest(
        Long classId,
        Long streamId,
        Long subjectId,
        Long termId,
        String title,
        String type,
        LocalDate assessmentDate,
        BigDecimal maxScore,
        List<Long> topicIds
) {
}
