package com.musomi.desktop.controller.teacher;

import java.util.List;

/**
 * Persisted representation of an {@link AssessmentStore.Assessment} and its marks.
 */
public record AssessmentDto(
        String name,
        String className,
        String subject,
        String topic,
        String date,
        int maxMarks,
        String status,
        List<StudentMarkDto> marks
) {
}
