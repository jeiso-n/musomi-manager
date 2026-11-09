package com.musomi.manager.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Historical class performance summary built from generated report snapshots. */
public record ClassPerformanceSummaryResponse(
        Long classId,
        String className,
        Long streamId,
        String streamName,
        Long termId,
        String termName,
        Integer academicYear,
        int totalStudents,
        int completeReports,
        int partialReports,
        int noResultsReports,
        BigDecimal classAverage,
        Map<String, Integer> gradeDistribution,
        List<SubjectPerformance> subjects,
        List<StudentPerformance> students
) {
    public record SubjectPerformance(
            String subjectName,
            BigDecimal average,
            int studentsWithMarks,
            int studentsMissingMarks,
            int expectedAssessmentCount,
            int recordedAssessmentCount
    ) {
    }

    public record StudentPerformance(
            Long studentId,
            String studentName,
            String admissionNumber,
            String reportStatus,
            BigDecimal overallAverage,
            String overallGrade,
            Integer position,
            Integer rankingCohortSize,
            Integer expectedAssessmentCount,
            Integer recordedAssessmentCount
    ) {
    }
}
