package com.musomi.desktop.controller.teacher;

/**
 * Persisted representation of {@link AssessmentStore.StudentMark}.
 */
public record StudentMarkDto(String number, String student, String mark) {
}
