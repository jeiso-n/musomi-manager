package com.musomi.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssessmentResponse(
        Long id,
        String title,
        String type,
        LocalDate assessmentDate,
        BigDecimal maxScore,
        String status,
        Long classId,
        String className,
        Long streamId,
        String streamName,
        Long subjectId,
        String subjectName,
        Long termId,
        String termName,
        List<TopicSummary> topics
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopicSummary(Long id, String name) {
    }
}
