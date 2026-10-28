package com.musomi.desktop.controller.teacher;

import java.util.List;

/**
 * Top-level document written to {@code ~/.musomi/assessments.json}.
 * The version is a schema version, currently 1.
 */
public record AssessmentsDocument(int version, List<AssessmentDto> assessments) {
}
