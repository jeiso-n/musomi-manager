package com.musomi.manager.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.musomi.manager.dto.response.ClassPerformanceSummaryResponse;

import com.musomi.manager.dto.response.ReportResponse;
import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Comment;
import com.musomi.manager.entity.GeneratedReport;
import com.musomi.manager.entity.GeneratedReportSubject;
import com.musomi.manager.entity.ReportRequest;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.SchoolSettings;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import com.musomi.manager.entity.User;
import com.musomi.manager.exception.AuthenticationException;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.PermissionDeniedException;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.exception.ValidationException;
import com.musomi.manager.repository.AssessmentRepository;
import com.musomi.manager.repository.CommentRepository;
import com.musomi.manager.repository.GeneratedReportRepository;
import com.musomi.manager.repository.ReportRequestRepository;
import com.musomi.manager.repository.SchoolSettingsRepository;
import com.musomi.manager.repository.SchoolRepository;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.ClassRepository;
import com.musomi.manager.repository.TeacherAssignmentRepository;
import com.musomi.manager.util.SchoolScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages generated report records and requests. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final GeneratedReportRepository generatedReportRepository;
    private final ReportRequestRepository reportRequestRepository;
    private final StudentRepository studentRepository;
    private final TermRepository termRepository;
    private final ClassRepository classRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final CommentRepository commentRepository;
    private final SchoolSettingsRepository schoolSettingsRepository;
    private final SchoolRepository schoolRepository;
    private final PdfService pdfService;

    /** Generates PDF report cards for active students in a class and stream. */
    @Transactional
    public List<ReportResponse> generateForClassStreamTerm(Long schoolId, Long classId, Long streamId, Long termId,
                                                           Long generatedBy, String requesterRole) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        if (generatedBy == null || generatedBy <= 0) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (term.getSchool() == null || !resolvedSchoolId.equals(term.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }
        classRepository.findById(classId)
                .filter(clazz -> clazz.getSchool() != null
                        && resolvedSchoolId.equals(clazz.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if ("TEACHER".equalsIgnoreCase(requesterRole)
                && !isTeacherAssignedToClassStream(resolvedSchoolId, generatedBy, classId, streamId, term)) {
            throw new PermissionDeniedException(ErrorCode.NOT_YOUR_SUBJECT,
                    "You are not assigned to the selected class and stream for this academic year.");
        }
        School school = schoolRepository.findById(resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        SchoolSettings settings = schoolSettingsRepository.findBySchoolId(resolvedSchoolId).orElse(null);
        List<Student> students = studentRepository.findByCurrentClassIdAndIsActiveTrue(classId).stream()
                .filter(student -> belongsToSchoolAndStream(student, resolvedSchoolId, streamId))
                .toList();
        if (students.isEmpty()) {
            throw new ValidationException(ErrorCode.NO_MARKS_PUBLISHED);
        }

        Map<Long, List<Score>> scoresByStudent = new HashMap<>();
        for (Student student : students) {
            scoresByStudent.put(student.getId(), scoreRepository.findByStudentId(student.getId()));
        }

        // Discover expected assessments independently of score rows. A missing score must not
        // cause a published subject or assessment to disappear from the report.
        List<Assessment> publishedAssessments = findPublishedAssessments(
                resolvedSchoolId, classId, streamId, termId);
        List<StudentReport> reports = students.stream()
                .map(student -> {
                    List<Assessment> studentAssessments = publishedAssessments.stream()
                            .filter(assessment -> assessment.getStream() == null
                                    || student.getCurrentStream() != null
                                    && assessment.getStream().getId().equals(student.getCurrentStream().getId()))
                            .toList();
                    return calculateStudentReport(student, studentAssessments,
                            scoresByStudent.getOrDefault(student.getId(), List.of()));
                })
                .toList();
        Map<Long, Integer> positions = calculatePositions(reports);
        int rankedStudentCount = (int) reports.stream().filter(StudentReport::complete).count();
        List<ReportResponse> responses = new ArrayList<>();
        for (StudentReport report : reports) {
            responses.add(generateStudentReport(
                    report, term, school, settings, resolvedSchoolId, termId, generatedBy,
                    positions.get(report.student().getId()), rankedStudentCount));
        }
        return List.copyOf(responses);
    }

    /** Lists generated reports for a student. */
    @Transactional(readOnly = true)
    public List<ReportResponse> listByStudent(Long schoolId, Long studentId, Long termId) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        studentRepository.findByIdAndSchoolId(studentId, resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
        List<GeneratedReport> reports = termId == null
                ? generatedReportRepository.findBySchoolIdAndStudentIdOrderByGeneratedAtDesc(
                        resolvedSchoolId, studentId)
                : generatedReportRepository.findBySchoolIdAndStudentIdAndTermIdOrderByGeneratedAtDesc(
                        resolvedSchoolId, studentId, termId);
        return reports.stream().map(this::toResponse).toList();
    }

    /** Returns the saved class performance snapshot for a class, stream and term. */
    @Transactional(readOnly = true)
    public ClassPerformanceSummaryResponse getClassPerformanceSummary(Long schoolId, Long classId,
                                                                       Long streamId, Long termId,
                                                                       Long requesterId, String requesterRole) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        Term term = termRepository.findById(termId)
                .filter(value -> value.getSchool() != null
                        && resolvedSchoolId.equals(value.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        var classEntity = classRepository.findById(classId)
                .filter(value -> value.getSchool() != null
                        && resolvedSchoolId.equals(value.getSchool().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if ("TEACHER".equalsIgnoreCase(requesterRole)
                && !isTeacherAssignedToClassStream(resolvedSchoolId, requesterId, classId, streamId, term)) {
            throw new PermissionDeniedException(ErrorCode.NOT_YOUR_SUBJECT,
                    "You are not assigned to the selected class and stream for this academic year.");
        }

        Map<Long, GeneratedReport> latestByStudent = new LinkedHashMap<>();
        for (GeneratedReport report : generatedReportRepository
                .findBySchoolIdAndClassIdAndTermIdOrderByGeneratedAtDesc(
                        resolvedSchoolId, classId, termId)) {
            if (streamId != null && !streamId.equals(report.getStreamId())) {
                continue;
            }
            if (report.getStudent() != null) {
                latestByStudent.putIfAbsent(report.getStudent().getId(), report);
            }
        }
        List<GeneratedReport> reports = List.copyOf(latestByStudent.values());
        List<GeneratedReport> completeReports = reports.stream()
                .filter(report -> "COMPLETE".equalsIgnoreCase(report.getReportStatus()))
                .toList();

        BigDecimal classAverage = average(completeReports.stream()
                .map(GeneratedReport::getOverallAverage).filter(value -> value != null).toList());
        Map<String, Integer> gradeDistribution = new LinkedHashMap<>();
        for (String grade : List.of("A", "B", "C", "D", "E", "F")) {
            gradeDistribution.put(grade, 0);
        }
        completeReports.stream().map(GeneratedReport::getOverallGrade)
                .filter(grade -> grade != null && gradeDistribution.containsKey(grade))
                .forEach(grade -> gradeDistribution.computeIfPresent(grade, (key, count) -> count + 1));

        Map<String, SubjectSnapshotAccumulator> subjectTotals = new LinkedHashMap<>();
        for (GeneratedReport report : reports) {
            for (GeneratedReportSubject subject : report.getSubjectResults()) {
                subjectTotals.computeIfAbsent(subject.getSubjectName(),
                        ignored -> new SubjectSnapshotAccumulator()).add(subject);
            }
        }
        List<ClassPerformanceSummaryResponse.SubjectPerformance> subjectPerformance = subjectTotals.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();

        List<ClassPerformanceSummaryResponse.StudentPerformance> studentPerformance = reports.stream()
                .map(report -> new ClassPerformanceSummaryResponse.StudentPerformance(
                        report.getStudent().getId(),
                        report.getStudent().getFullName(),
                        report.getStudent().getAdmissionNumber(),
                        report.getReportStatus() == null ? "LEGACY" : report.getReportStatus(),
                        report.getOverallAverage(),
                        report.getOverallGrade(),
                        report.getClassPosition(),
                        report.getRankingCohortSize(),
                        report.getExpectedAssessmentCount(),
                        report.getRecordedAssessmentCount()))
                .sorted(Comparator.comparing(
                        ClassPerformanceSummaryResponse.StudentPerformance::overallAverage,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        String className = reports.stream().map(GeneratedReport::getClassName)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(classEntity.getName());
        String streamName = streamId == null ? "All streams" : reports.stream()
                .map(GeneratedReport::getStreamName)
                .filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(null);
        int completeCount = (int) reports.stream()
                .filter(report -> "COMPLETE".equalsIgnoreCase(report.getReportStatus())).count();
        int noResultsCount = (int) reports.stream()
                .filter(report -> "NO_RESULTS".equalsIgnoreCase(report.getReportStatus())).count();
        return new ClassPerformanceSummaryResponse(
                classId, className, streamId, streamName,
                termId, term.getName(),
                term.getAcademicYear() == null ? null : term.getAcademicYear().getYear(),
                reports.size(), completeCount, reports.size() - completeCount - noResultsCount,
                noResultsCount, classAverage, gradeDistribution, subjectPerformance, studentPerformance);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private static final class SubjectSnapshotAccumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private int studentsWithMarks;
        private int studentsMissingMarks;
        private int expectedCount;
        private int recordedCount;

        private void add(GeneratedReportSubject subject) {
            int expected = subject.getExpectedAssessmentCount() == null ? 0 : subject.getExpectedAssessmentCount();
            int recorded = subject.getRecordedAssessmentCount() == null ? 0 : subject.getRecordedAssessmentCount();
            expectedCount += expected;
            recordedCount += recorded;
            if (recorded < expected) {
                studentsMissingMarks++;
            }
            if (subject.getAverage() != null) {
                total = total.add(subject.getAverage());
                studentsWithMarks++;
            }
        }

        private ClassPerformanceSummaryResponse.SubjectPerformance toResponse(String subjectName) {
            BigDecimal subjectAverage = studentsWithMarks == 0 ? null
                    : total.divide(BigDecimal.valueOf(studentsWithMarks), 1, RoundingMode.HALF_UP);
            return new ClassPerformanceSummaryResponse.SubjectPerformance(
                    subjectName, subjectAverage, studentsWithMarks, studentsMissingMarks,
                    expectedCount, recordedCount);
        }
    }

    /** Returns a generated report by id. */
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long id, Long schoolId) {
        GeneratedReport report = generatedReportRepository.findByIdAndSchoolId(id, SchoolScope.require(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND));
        return toResponse(report);
    }

    /** Returns generated report metadata for file download. */
    @Transactional(readOnly = true)
    public GeneratedReport getGeneratedReport(Long id, Long schoolId) {
        return generatedReportRepository.findByIdAndSchoolId(id, SchoolScope.require(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND));
    }

    /** Creates a pending report request when no pending request exists for the student and term. */
    @Transactional
    public ReportRequest createReportRequest(Long schoolId, Long studentId, Long termId, Long requestedBy) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        if (requestedBy == null || requestedBy <= 0) {
            throw new com.musomi.manager.exception.AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (term.getSchool() == null || !resolvedSchoolId.equals(term.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }
        if (reportRequestRepository.findByStudentIdAndTermIdAndStatus(studentId, termId, "PENDING").isPresent()) {
            throw new ValidationException(ErrorCode.REPORT_ALREADY_GENERATED);
        }

        ReportRequest request = ReportRequest.builder()
                .school(School.builder().id(resolvedSchoolId).build())
                .student(student)
                .term(term)
                .status("PENDING")
                .requestedBy(User.builder().id(requestedBy).build())
                .requestedAt(LocalDateTime.now())
                .build();
        ReportRequest saved = reportRequestRepository.save(request);
        log.info("Report request {} created for student {} and term {}", saved.getId(), studentId, termId);
        return saved;
    }

    /** Marks a report request generated after its report has been recorded. */
    @Transactional
    public void markGenerated(Long requestId, Long reportId, Long schoolId) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        ReportRequest request = reportRequestRepository.findByIdAndSchoolId(requestId, resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND));
        GeneratedReport report = generatedReportRepository.findByIdAndSchoolId(reportId, resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND));
        if (request.getStudent() == null || report.getStudent() == null
                || !request.getStudent().getId().equals(report.getStudent().getId())
                || request.getTerm() == null || report.getTerm() == null
                || !request.getTerm().getId().equals(report.getTerm().getId())) {
            throw new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND);
        }
        request.setStatus("GENERATED");
        request.setProcessedAt(LocalDateTime.now());
        reportRequestRepository.save(request);
        log.info("Report request {} marked generated with report {}", requestId, reportId);
    }

    /** Records generated report metadata without generating a PDF. */
    @Transactional
    public GeneratedReport recordGenerated(Long schoolId, Long studentId, Long termId, String filePath,
                                           Long fileSizeBytes, Long generatedBy) {
        Long resolvedSchoolId = SchoolScope.require(schoolId);
        if (generatedBy == null || generatedBy <= 0) {
            throw new AuthenticationException(ErrorCode.SESSION_EXPIRED);
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, resolvedSchoolId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED));
        if (term.getSchool() == null || !resolvedSchoolId.equals(term.getSchool().getId())) {
            throw new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED);
        }

        GeneratedReport report = generatedReportRepository.save(GeneratedReport.builder()
                .school(School.builder().id(resolvedSchoolId).build())
                .student(student)
                .term(term)
                .filePath(filePath)
                .fileSizeBytes(fileSizeBytes)
                .generatedBy(User.builder().id(generatedBy).build())
                .generatedAt(LocalDateTime.now())
                .build());
        log.info("Report {} recorded for student {} and term {}", report.getId(), studentId, termId);
        return report;
    }

    private ReportResponse toResponse(GeneratedReport report) {
        Student student = report.getStudent();
        Term term = report.getTerm();
        return new ReportResponse(
                report.getId(),
                student != null ? student.getId() : null,
                student != null ? student.getFullName() : null,
                term != null ? term.getId() : null,
                term != null ? term.getName() : null,
                term != null && term.getAcademicYear() != null ? term.getAcademicYear().getYear() : null,
                report.getFileSizeBytes(),
                report.getGeneratedAt(),
                "/reports/download/" + report.getId(),
                report.getClassId(),
                report.getStreamId(),
                report.getReportStatus(),
                report.getExpectedAssessmentCount(),
                report.getRecordedAssessmentCount(),
                report.getOverallAverage(),
                report.getOverallGrade(),
                report.getClassPosition(),
                report.getRankingCohortSize()
        );
    }

    private boolean belongsToSchoolAndStream(Student student, Long schoolId, Long streamId) {
        if (student.getSchool() == null || !schoolId.equals(student.getSchool().getId())
                || !Boolean.TRUE.equals(student.getIsActive())) {
            return false;
        }
        return streamId == null
                || student.getCurrentStream() != null && streamId.equals(student.getCurrentStream().getId());
    }

    private boolean isTeacherAssignedToClassStream(Long schoolId, Long teacherId, Long classId,
                                                   Long streamId, Term term) {
        if (term.getAcademicYear() == null || term.getAcademicYear().getId() == null) {
            return false;
        }
        return teacherAssignmentRepository.findBySchoolIdAndTeacherIdOrderByIdAsc(schoolId, teacherId).stream()
                .filter(assignment -> assignment.getClassEntity() != null
                        && classId.equals(assignment.getClassEntity().getId()))
                .filter(assignment -> assignment.getAcademicYear() != null
                        && term.getAcademicYear().getId().equals(assignment.getAcademicYear().getId()))
                .anyMatch(assignment -> {
                    Long assignedStreamId = assignment.getStream() == null
                            ? null : assignment.getStream().getId();
                    return streamId == null
                            ? assignedStreamId == null
                            : assignedStreamId == null || streamId.equals(assignedStreamId);
                });
    }

    private List<Assessment> findPublishedAssessments(Long schoolId, Long classId,
                                                      Long streamId, Long termId) {
        return assessmentRepository.findByClassEntityIdAndTermId(classId, termId).stream()
                .filter(assessment -> isEligibleAssessment(assessment, schoolId, classId, streamId, termId))
                .filter(assessment -> assessment.getId() != null)
                .sorted(Comparator.comparing(Assessment::getId))
                .toList();
    }

    private StudentReport calculateStudentReport(Student student, List<Assessment> assessments, List<Score> scores) {
        Map<Long, Score> scoresByAssessment = new HashMap<>();
        for (Score score : scores) {
            if (score.getAssessment() != null && score.getAssessment().getId() != null) {
                scoresByAssessment.put(score.getAssessment().getId(), score);
            }
        }

        Map<Long, SubjectTotals> totalsBySubject = new HashMap<>();
        for (Assessment assessment : assessments) {
            if (assessment.getSubject() == null || assessment.getSubject().getId() == null) {
                continue;
            }
            Long subjectId = assessment.getSubject().getId();
            SubjectTotals totals = totalsBySubject.computeIfAbsent(subjectId,
                    ignored -> new SubjectTotals(assessment.getSubject().getName()));
            totals.expectAssessment();
            Score score = scoresByAssessment.get(assessment.getId());
            if (score == null || score.getScore() == null || assessment.getMaxScore() == null
                    || assessment.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // A recorded zero is a real mark. Only an absent/null score is missing.
            BigDecimal percentage = score.getScore().multiply(BigDecimal.valueOf(100))
                    .divide(assessment.getMaxScore(), 4, RoundingMode.HALF_UP);
            totals.add(percentage);
        }

        List<PdfService.SubjectAverage> subjects = totalsBySubject.values().stream()
                .map(SubjectTotals::toAverage)
                .sorted(Comparator.comparing(PdfService.SubjectAverage::subjectName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<BigDecimal> availableSubjectAverages = subjects.stream()
                .map(PdfService.SubjectAverage::average)
                .filter(average -> average != null)
                .toList();
        BigDecimal overallAverage = availableSubjectAverages.isEmpty() ? null
                : availableSubjectAverages.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(availableSubjectAverages.size()), 1, RoundingMode.HALF_UP);
        String overallGrade = com.musomi.manager.util.GradeCalculator.calculate(
                overallAverage, BigDecimal.valueOf(100));
        int recordedAssessmentCount = totalsBySubject.values().stream().mapToInt(SubjectTotals::getCount).sum();
        int expectedAssessmentCount = assessments.size();
        boolean complete = expectedAssessmentCount > 0 && recordedAssessmentCount == expectedAssessmentCount;
        return new StudentReport(student, subjects, overallAverage, overallGrade, complete,
                expectedAssessmentCount, recordedAssessmentCount);
    }

    private boolean isEligibleAssessment(Assessment assessment, Long schoolId, Long classId, Long streamId,
                                         Long termId) {
        return assessment != null
                && "PUBLISHED".equalsIgnoreCase(assessment.getStatus())
                && assessment.getSchool() != null && schoolId.equals(assessment.getSchool().getId())
                && assessment.getClassEntity() != null && classId.equals(assessment.getClassEntity().getId())
                && assessment.getTerm() != null && termId.equals(assessment.getTerm().getId())
                && (assessment.getStream() == null
                        || streamId == null || streamId.equals(assessment.getStream().getId()));
    }

    private Map<Long, Integer> calculatePositions(List<StudentReport> reports) {
        List<StudentReport> ranked = reports.stream()
                .filter(StudentReport::complete)
                .filter(report -> report.overallAverage() != null)
                .sorted(Comparator.comparing(StudentReport::overallAverage).reversed())
                .toList();
        Map<Long, Integer> positions = new HashMap<>();
        BigDecimal previousAverage = null;
        int position = 0;
        for (int index = 0; index < ranked.size(); index++) {
            StudentReport report = ranked.get(index);
            if (previousAverage == null || previousAverage.compareTo(report.overallAverage()) != 0) {
                position = index + 1;
                previousAverage = report.overallAverage();
            }
            positions.put(report.student().getId(), position);
        }
        return positions;
    }

    private ReportResponse generateStudentReport(StudentReport report, Term term, School school,
                                                 SchoolSettings settings,
                                                 Long schoolId, Long termId, Long generatedBy, Integer position,
                                                 int classSize) {
        byte[] pdf = pdfService.generateReportCard(
                report.student(), term, school, settings, report.subjects(), report.overallAverage(), report.overallGrade(),
                position == null ? 0 : position, classSize,
                commentRepository.findByStudentIdAndTermIdOrderByWrittenAtDesc(report.student().getId(), termId));
        Path filePath = reportFilePath(report.student(), term, schoolId, termId);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, pdf);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write generated report card to " + filePath, exception);
        }
        String storedFilePath = filePath.toString().replace('\\', '/');

        int expectedCount = report.expectedAssessmentCount();
        int recordedCount = report.recordedAssessmentCount();
        String status = report.subjects().isEmpty() ? "NO_RESULTS"
                : report.complete() ? "COMPLETE" : "PARTIAL";
        GeneratedReport saved = generatedReportRepository.save(GeneratedReport.builder()
                .school(School.builder().id(schoolId).build())
                .student(report.student())
                .term(term)
                .filePath(storedFilePath)
                .fileSizeBytes((long) pdf.length)
                .generatedBy(User.builder().id(generatedBy).build())
                .generatedAt(LocalDateTime.now())
                .classId(report.student().getCurrentClass() == null
                        ? null : report.student().getCurrentClass().getId())
                .streamId(report.student().getCurrentStream() == null
                        ? null : report.student().getCurrentStream().getId())
                .className(report.student().getCurrentClass() == null
                        ? null : report.student().getCurrentClass().getName())
                .streamName(report.student().getCurrentStream() == null
                        ? null : report.student().getCurrentStream().getName())
                .reportStatus(status)
                .expectedAssessmentCount(expectedCount)
                .recordedAssessmentCount(recordedCount)
                .overallAverage(report.overallAverage())
                .overallGrade(report.overallGrade())
                .classPosition(position)
                .rankingCohortSize(position == null ? null : classSize)
                .build());
        GeneratedReport parentReport = saved;
        List<GeneratedReportSubject> subjectSnapshots = report.subjects().stream()
                .map(subject -> GeneratedReportSubject.builder()
                        .generatedReport(parentReport)
                        .subjectName(subject.subjectName())
                        .average(subject.average())
                        .grade(subject.grade())
                        .expectedAssessmentCount(subject.expectedAssessmentCount())
                        .recordedAssessmentCount(subject.assessmentCount())
                        .build())
                .toList();
        saved.setSubjectResults(subjectSnapshots);
        saved = generatedReportRepository.save(saved);
        log.info("Generated report {} for student {} in term {}", saved.getId(),
                report.student().getId(), termId);
        return toResponse(saved);
    }

    private Path reportFilePath(Student student, Term term, Long schoolId, Long termId) {
        String year = term.getAcademicYear() == null || term.getAcademicYear().getYear() == null
                ? "unknown"
                : term.getAcademicYear().getYear().toString();
        String admissionNumber = student.getAdmissionNumber() == null || student.getAdmissionNumber().isBlank()
                ? "student-" + student.getId()
                : student.getAdmissionNumber().replaceAll("[^A-Za-z0-9._-]", "_");
        return Path.of("data", "reports", schoolId.toString(), year, termId.toString(), admissionNumber + ".pdf");
    }

    private record StudentReport(Student student, List<PdfService.SubjectAverage> subjects,
                                 BigDecimal overallAverage, String overallGrade, boolean complete,
                                 int expectedAssessmentCount, int recordedAssessmentCount) {
    }

    private static final class SubjectTotals {
        private final String subjectName;
        private BigDecimal total = BigDecimal.ZERO;
        private int count;
        private int expectedCount;

        private SubjectTotals(String subjectName) {
            this.subjectName = subjectName;
        }

        private void expectAssessment() {
            expectedCount++;
        }

        private void add(BigDecimal score) {
            total = total.add(score);
            count++;
        }

        private int getCount() {
            return count;
        }

        private PdfService.SubjectAverage toAverage() {
            BigDecimal average = count == 0 ? null
                    : total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
            return new PdfService.SubjectAverage(subjectName, average,
                    com.musomi.manager.util.GradeCalculator.calculate(average, BigDecimal.valueOf(100)),
                    count, expectedCount);
        }
    }
}
