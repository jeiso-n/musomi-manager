package com.musomi.desktop.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.musomi.desktop.model.AssessmentResponse;
import com.musomi.desktop.model.dto.TeacherClassResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardControllerTest {

    @Test
    @DisplayName("calculateTeacherClassStats deduplicates students across multiple subjects in same class")
    void calculateTeacherClassStats_deduplicatesStudentsAcrossSubjects() {
        List<TeacherClassResponse> classes = List.of(
                new TeacherClassResponse(1L, "S3", null, null, 1L, "Mathematics", 50, 1, 1),
                new TeacherClassResponse(1L, "S3", null, null, 2L, "Physics", 50, 0, 0),
                new TeacherClassResponse(2L, "S4", 3L, "Red", 1L, "Mathematics", 30, 1, 0)
        );

        DashboardController.TeacherClassStats stats =
                DashboardController.calculateTeacherClassStats(classes);

        assertEquals(80, stats.totalStudents());
        assertEquals(2, stats.totalClasses());
    }

    @Test
    @DisplayName("calculateTeacherClassStats avoids double counting when whole-class and stream assignments coexist")
    void calculateTeacherClassStats_avoidsWholeClassAndStreamDoubleCounting() {
        List<TeacherClassResponse> classes = List.of(
                new TeacherClassResponse(1L, "S3", null, null, 1L, "Mathematics", 50, 1, 1),
                new TeacherClassResponse(1L, "S3", 2L, "Blue", 2L, "Physics", 25, 0, 0)
        );

        DashboardController.TeacherClassStats stats =
                DashboardController.calculateTeacherClassStats(classes);

        assertEquals(50, stats.totalStudents());
        assertEquals(1, stats.totalClasses());
    }

    @Test
    @DisplayName("calculateTeacherAssessmentStats counts total, draft, and published assessments")
    void calculateTeacherAssessmentStats_countsDraftAndPublished() {
        List<AssessmentResponse> assessments = List.of(
                new AssessmentResponse(1L, "BOT Math", "BOT", null, java.math.BigDecimal.valueOf(20),
                        "DRAFT", 1L, "S3", null, null, 1L, "Mathematics", 3L, "Term 1", List.of()),
                new AssessmentResponse(2L, "MOT Physics", "MOT", null, java.math.BigDecimal.valueOf(30),
                        "PUBLISHED", 1L, "S3", null, null, 2L, "Physics", 3L, "Term 1", List.of()),
                new AssessmentResponse(3L, "EOT Math", "EOT", null, java.math.BigDecimal.valueOf(80),
                        "PUBLISHED", 2L, "S4", null, null, 1L, "Mathematics", 3L, "Term 1", List.of())
        );

        DashboardController.TeacherAssessmentStats stats =
                DashboardController.calculateTeacherAssessmentStats(assessments);

        assertEquals(3, stats.total());
        assertEquals(1, stats.draft());
        assertEquals(2, stats.published());
    }
}

