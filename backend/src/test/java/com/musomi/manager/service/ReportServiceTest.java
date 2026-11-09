package com.musomi.manager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.musomi.manager.dto.response.ClassPerformanceSummaryResponse;
import com.musomi.manager.dto.response.ReportResponse;
import com.musomi.manager.entity.AcademicYear;
import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.ClassEntity;
import com.musomi.manager.entity.GeneratedReport;
import com.musomi.manager.entity.GeneratedReportSubject;
import com.musomi.manager.entity.ReportRequest;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.TeacherAssignment;
import com.musomi.manager.entity.Stream;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.exception.ErrorCode;
import com.musomi.manager.exception.ResourceNotFoundException;
import com.musomi.manager.exception.ValidationException;
import com.musomi.manager.repository.GeneratedReportRepository;
import com.musomi.manager.repository.ClassRepository;
import com.musomi.manager.repository.ReportRequestRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.TeacherAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long SCHOOL_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long TERM_ID = 3L;
    private static final Long USER_ID = 4L;

    @Mock
    private GeneratedReportRepository generatedReportRepository;

    @Mock
    private ReportRequestRepository reportRequestRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("should list reports by student")
    void shouldListReportsByStudent() {
        when(studentRepository.findByIdAndSchoolId(STUDENT_ID, SCHOOL_ID)).thenReturn(Optional.of(student()));
        when(generatedReportRepository.findBySchoolIdAndStudentIdOrderByGeneratedAtDesc(SCHOOL_ID, STUDENT_ID))
                .thenReturn(List.of(report()));

        List<ReportResponse> response = reportService.listByStudent(SCHOOL_ID, STUDENT_ID, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).studentName()).isEqualTo("Alex Student");
        assertThat(response.get(0).academicYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("should filter generated reports by term when term id is supplied")
    void shouldFilterReportsByTerm() {
        when(studentRepository.findByIdAndSchoolId(STUDENT_ID, SCHOOL_ID)).thenReturn(Optional.of(student()));
        when(generatedReportRepository.findBySchoolIdAndStudentIdAndTermIdOrderByGeneratedAtDesc(
                SCHOOL_ID, STUDENT_ID, TERM_ID)).thenReturn(List.of(report()));

        List<ReportResponse> response = reportService.listByStudent(SCHOOL_ID, STUDENT_ID, TERM_ID);

        assertThat(response).hasSize(1);
        verify(generatedReportRepository).findBySchoolIdAndStudentIdAndTermIdOrderByGeneratedAtDesc(
                SCHOOL_ID, STUDENT_ID, TERM_ID);
    }

    @Test
    @DisplayName("should summarize class performance from the latest saved report snapshots")
    void shouldSummarizeClassPerformance() {
        Student completeStudent = Student.builder().id(10L).school(school())
                .fullName("Alex Complete").admissionNumber("A-010").build();
        Student partialStudent = Student.builder().id(11L).school(school())
                .fullName("Sam Partial").admissionNumber("A-011").build();
        GeneratedReport complete = GeneratedReport.builder()
                .id(100L).school(school()).student(completeStudent).term(term())
                .classId(20L).reportStatus("COMPLETE")
                .expectedAssessmentCount(2).recordedAssessmentCount(2)
                .overallAverage(new BigDecimal("80.0")).overallGrade("A")
                .classPosition(1).rankingCohortSize(1)
                .subjectResults(List.of(GeneratedReportSubject.builder()
                        .subjectName("Mathematics").average(new BigDecimal("80.0")).grade("A")
                        .expectedAssessmentCount(2).recordedAssessmentCount(2).build()))
                .build();
        GeneratedReport partial = GeneratedReport.builder()
                .id(101L).school(school()).student(partialStudent).term(term())
                .classId(20L).reportStatus("PARTIAL")
                .expectedAssessmentCount(2).recordedAssessmentCount(1)
                .overallAverage(new BigDecimal("50.0")).overallGrade("D")
                .subjectResults(List.of(GeneratedReportSubject.builder()
                        .subjectName("Mathematics").average(new BigDecimal("50.0")).grade("D")
                        .expectedAssessmentCount(2).recordedAssessmentCount(1).build()))
                .build();
        when(termRepository.findById(TERM_ID)).thenReturn(Optional.of(term()));
        when(classRepository.findById(20L)).thenReturn(Optional.of(
                ClassEntity.builder().id(20L).school(school()).name("Primary 1").build()));
        when(generatedReportRepository.findBySchoolIdAndClassIdAndTermIdOrderByGeneratedAtDesc(
                SCHOOL_ID, 20L, TERM_ID)).thenReturn(List.of(complete, partial));

        ClassPerformanceSummaryResponse response = reportService.getClassPerformanceSummary(
                SCHOOL_ID, 20L, null, TERM_ID, USER_ID, "ADMIN");

        assertThat(response.totalStudents()).isEqualTo(2);
        assertThat(response.completeReports()).isEqualTo(1);
        assertThat(response.partialReports()).isEqualTo(1);
        assertThat(response.noResultsReports()).isZero();
        assertThat(response.classAverage()).isEqualByComparingTo("80.0");
        assertThat(response.gradeDistribution()).containsEntry("A", 1);
        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).studentsWithMarks()).isEqualTo(2);
        assertThat(response.subjects().get(0).studentsMissingMarks()).isEqualTo(1);
        assertThat(response.students()).hasSize(2);
    }

    @Test
    @DisplayName("should keep recorded zero marks and label incomplete reports")
    void shouldCalculatePartialReportWithoutTreatingZeroAsMissing() {
        Subject subject = Subject.builder().id(10L).name("Mathematics").build();
        Assessment first = Assessment.builder().id(101L).subject(subject)
                .maxScore(new BigDecimal("100")).build();
        Assessment second = Assessment.builder().id(102L).subject(subject)
                .maxScore(new BigDecimal("100")).build();
        Score zeroMark = Score.builder().assessment(first).score(BigDecimal.ZERO).build();

        Object result = ReflectionTestUtils.invokeMethod(reportService, "calculateStudentReport",
                student(), List.of(first, second), List.of(zeroMark));

        assertThat(ReflectionTestUtils.getField(result, "complete")).isEqualTo(false);
        assertThat(ReflectionTestUtils.getField(result, "overallAverage")).isEqualTo(new BigDecimal("0.0"));
        @SuppressWarnings("unchecked")
        List<com.musomi.manager.service.PdfService.SubjectAverage> subjects =
                (List<com.musomi.manager.service.PdfService.SubjectAverage>)
                        ReflectionTestUtils.getField(result, "subjects");
        assertThat(subjects).hasSize(1);
        assertThat(subjects.get(0).average()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(subjects.get(0).assessmentCount()).isEqualTo(1);
        assertThat(subjects.get(0).expectedAssessmentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should generate a partial no-results calculation when no assessments are published")
    void shouldCalculateNoResultsWithoutThrowing() {
        Object result = ReflectionTestUtils.invokeMethod(reportService, "calculateStudentReport",
                student(), List.of(), List.of());

        assertThat(ReflectionTestUtils.getField(result, "complete")).isEqualTo(false);
        assertThat(ReflectionTestUtils.getField(result, "overallAverage")).isNull();
        assertThat(ReflectionTestUtils.getField(result, "overallGrade")).isNull();
        assertThat(ReflectionTestUtils.getField(result, "subjects")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("should mark report complete when every published assessment has a score")
    void shouldCalculateCompleteReport() {
        Subject subject = Subject.builder().id(10L).name("Mathematics").build();
        Assessment first = Assessment.builder().id(101L).subject(subject)
                .maxScore(new BigDecimal("100")).build();
        Assessment second = Assessment.builder().id(102L).subject(subject)
                .maxScore(new BigDecimal("100")).build();
        List<Score> scores = List.of(
                Score.builder().assessment(first).score(BigDecimal.ZERO).build(),
                Score.builder().assessment(second).score(new BigDecimal("50")).build());

        Object result = ReflectionTestUtils.invokeMethod(reportService, "calculateStudentReport",
                student(), List.of(first, second), scores);

        assertThat(ReflectionTestUtils.getField(result, "complete")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(result, "overallAverage"))
                .isEqualTo(new BigDecimal("25.0"));
    }

    @Test
    @DisplayName("should authorize teacher only for assigned class, stream and academic year")
    void shouldCheckTeacherClassStreamAndAcademicYearAssignment() {
        AcademicYear academicYear = AcademicYear.builder().id(6L).year(2026).build();
        TeacherAssignment assignment = TeacherAssignment.builder()
                .classEntity(ClassEntity.builder().id(20L).build())
                .academicYear(academicYear)
                .stream(Stream.builder().id(30L).build())
                .build();
        when(teacherAssignmentRepository.findBySchoolIdAndTeacherIdOrderByIdAsc(SCHOOL_ID, USER_ID))
                .thenReturn(List.of(assignment));

        boolean assigned = ReflectionTestUtils.invokeMethod(reportService, "isTeacherAssignedToClassStream",
                SCHOOL_ID, USER_ID, 20L, 30L, term());
        boolean wrongStream = ReflectionTestUtils.invokeMethod(reportService, "isTeacherAssignedToClassStream",
                SCHOOL_ID, USER_ID, 20L, 31L, term());
        boolean wrongClass = ReflectionTestUtils.invokeMethod(reportService, "isTeacherAssignedToClassStream",
                SCHOOL_ID, USER_ID, 21L, 30L, term());
        Term wrongYearTerm = Term.builder().id(9L).school(school()).name("Term 1")
                .academicYear(AcademicYear.builder().id(7L).year(2025).build()).build();
        boolean wrongAcademicYear = ReflectionTestUtils.invokeMethod(reportService,
                "isTeacherAssignedToClassStream", SCHOOL_ID, USER_ID, 20L, 30L, wrongYearTerm);

        assertThat(assigned).isTrue();
        assertThat(wrongStream).isFalse();
        assertThat(wrongClass).isFalse();
        assertThat(wrongAcademicYear).isFalse();
    }

    @Test
    @DisplayName("should get report when it exists")
    void shouldGetReportWhenExists() {
        when(generatedReportRepository.findByIdAndSchoolId(5L, SCHOOL_ID)).thenReturn(Optional.of(report()));

        ReportResponse response = reportService.getReport(5L, SCHOOL_ID);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.downloadUrl()).isEqualTo("/reports/download/5");
    }

    @Test
    @DisplayName("should throw when report not found")
    void shouldThrowWhenReportNotFound() {
        when(generatedReportRepository.findByIdAndSchoolId(99L, SCHOOL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReport(99L, SCHOOL_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("should create request when none active")
    void shouldCreateRequestWhenNoneActive() {
        stubRequestParents();
        when(reportRequestRepository.findByStudentIdAndTermIdAndStatus(STUDENT_ID, TERM_ID, "PENDING"))
                .thenReturn(Optional.empty());
        when(reportRequestRepository.save(any(ReportRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReportRequest response = reportService.createReportRequest(SCHOOL_ID, STUDENT_ID, TERM_ID, USER_ID);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getStudent().getId()).isEqualTo(STUDENT_ID);
        verify(reportRequestRepository).save(any(ReportRequest.class));
    }

    @Test
    @DisplayName("should throw when active request exists")
    void shouldThrowWhenActiveRequestExists() {
        stubRequestParents();
        when(reportRequestRepository.findByStudentIdAndTermIdAndStatus(STUDENT_ID, TERM_ID, "PENDING"))
                .thenReturn(Optional.of(ReportRequest.builder().id(8L).status("PENDING").build()));

        assertThatThrownBy(() -> reportService.createReportRequest(SCHOOL_ID, STUDENT_ID, TERM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_ALREADY_GENERATED);
    }

    @Test
    @DisplayName("should mark generated and update status")
    void shouldMarkGeneratedAndUpdateStatus() {
        ReportRequest request = ReportRequest.builder().id(8L).status("PENDING").build();
        request.setSchool(school());
        request.setStudent(Student.builder().id(STUDENT_ID).school(school()).build());
        request.setTerm(term());
        when(reportRequestRepository.findByIdAndSchoolId(8L, SCHOOL_ID)).thenReturn(Optional.of(request));
        when(generatedReportRepository.findByIdAndSchoolId(5L, SCHOOL_ID)).thenReturn(Optional.of(report()));
        when(reportRequestRepository.save(any(ReportRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reportService.markGenerated(8L, 5L, SCHOOL_ID);

        assertThat(request.getStatus()).isEqualTo("GENERATED");
        assertThat(request.getProcessedAt()).isNotNull();
        verify(reportRequestRepository).save(request);
    }

    @Test
    @DisplayName("should record generated report")
    void shouldRecordGeneratedReport() {
        stubRequestParents();
        when(generatedReportRepository.save(any(GeneratedReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeneratedReport response = reportService.recordGenerated(
                SCHOOL_ID, STUDENT_ID, TERM_ID, "reports/term-1/student-2.pdf", 1024L, USER_ID);

        assertThat(response.getFilePath()).isEqualTo("reports/term-1/student-2.pdf");
        assertThat(response.getFileSizeBytes()).isEqualTo(1024L);
        assertThat(response.getGeneratedAt()).isNotNull();
        verify(generatedReportRepository).save(any(GeneratedReport.class));
    }

    private void stubRequestParents() {
        when(studentRepository.findByIdAndSchoolId(STUDENT_ID, SCHOOL_ID)).thenReturn(Optional.of(
                Student.builder().id(STUDENT_ID).school(school()).fullName("Alex Student").build()));
        when(termRepository.findById(TERM_ID)).thenReturn(Optional.of(term()));
    }

    private GeneratedReport report() {
        return GeneratedReport.builder()
                .id(5L)
                .school(school())
                .student(Student.builder().id(STUDENT_ID).school(school()).fullName("Alex Student").build())
                .term(term())
                .filePath("reports/term-1/student-2.pdf")
                .fileSizeBytes(1024L)
                .generatedAt(LocalDateTime.of(2026, 4, 20, 15, 0))
                .build();
    }

    private Student student() {
        return Student.builder().id(STUDENT_ID).school(school()).fullName("Alex Student").build();
    }

    private Term term() {
        return Term.builder()
                .id(TERM_ID)
                .school(school())
                .name("Term 1")
                .academicYear(AcademicYear.builder().id(6L).year(2026).build())
                .build();
    }

    private School school() {
        return School.builder().id(SCHOOL_ID).build();
    }
}
