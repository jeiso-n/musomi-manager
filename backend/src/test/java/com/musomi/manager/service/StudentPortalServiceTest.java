package com.musomi.manager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.ClassEntity;
import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Stream;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.repository.AssessmentRepository;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentPortalServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TermRepository termRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ScoreRepository scoreRepository;

    @InjectMocks private StudentPortalService service;

    private School school;
    private ClassEntity schoolClass;
    private Stream stream;
    private Term term;
    private Student student;
    private Subject mathematics;
    private Subject english;

    @BeforeEach
    void setUp() {
        school = School.builder().id(1L).build();
        schoolClass = ClassEntity.builder().id(10L).school(school).build();
        stream = Stream.builder().id(100L).school(school).classEntity(schoolClass).build();
        term = Term.builder().id(20L).school(school).build();
        student = Student.builder().id(501L).school(school).currentClass(schoolClass)
                .currentStream(stream).isActive(true).build();
        mathematics = Subject.builder().id(1L).name("Mathematics").code("MTH").build();
        english = Subject.builder().id(2L).name("English").code("ENG").build();
    }

    @Test
    void performanceUsesOnlyPublishedAssessmentsForCurrentClassStreamAndTerm() {
        Assessment classWideMath = assessment(101L, term, mathematics, null, "PUBLISHED", "100");
        Assessment streamMath = assessment(102L, term, mathematics, stream, "PUBLISHED", "100");
        Assessment otherStreamMath = assessment(103L, term, mathematics,
                Stream.builder().id(200L).school(school).classEntity(schoolClass).build(), "PUBLISHED", "100");
        Assessment draftEnglish = assessment(104L, term, english, null, "DRAFT", "100");
        Term previousTerm = Term.builder().id(19L).school(school).build();
        Assessment previousTermEnglish = assessment(105L, previousTerm, english, null, "PUBLISHED", "100");

        when(assessmentRepository.findByClassEntityIdAndTermId(10L, 20L))
                .thenReturn(List.of(classWideMath, streamMath, otherStreamMath, draftEnglish, previousTermEnglish));
        when(scoreRepository.findByStudentId(501L)).thenReturn(List.of(
                score(1L, classWideMath, "0"),
                score(2L, streamMath, "90"),
                score(3L, otherStreamMath, "100"),
                score(4L, previousTermEnglish, "100")));

        StudentPortalService.TermPerformance result = service.performance(student, term);

        assertThat(result.expectedAssessmentCount()).isEqualTo(2);
        assertThat(result.recordedAssessmentCount()).isEqualTo(2);
        assertThat(result.complete()).isTrue();
        assertThat(result.subjects()).hasSize(1);
        assertThat(result.subjects().get(0).average()).isEqualByComparingTo("45.0");
        assertThat(result.overallAverage()).isEqualByComparingTo("45.0");
        assertThat(result.overallGrade()).isEqualTo("E");
    }

    @Test
    void missingMarksRemainMissingAndPreventRankingInsteadOfBecomingZero() {
        Assessment math = assessment(201L, term, mathematics, null, "PUBLISHED", "100");
        Assessment englishAssessment = assessment(202L, term, english, null, "PUBLISHED", "100");
        when(assessmentRepository.findByClassEntityIdAndTermId(10L, 20L))
                .thenReturn(List.of(math, englishAssessment));
        when(scoreRepository.findByStudentId(501L)).thenReturn(List.of(score(5L, math, "80")));

        StudentPortalService.TermPerformance result = service.performance(student, term);
        StudentPortalService.Rank rank = service.rank(student, term, result);

        assertThat(result.expectedAssessmentCount()).isEqualTo(2);
        assertThat(result.recordedAssessmentCount()).isEqualTo(1);
        assertThat(result.complete()).isFalse();
        assertThat(result.subjects()).filteredOn(subject -> subject.subjectId().equals(2L))
                .singleElement().satisfies(subject -> assertThat(subject.average()).isNull());
        assertThat(result.overallAverage()).isEqualByComparingTo("80.0");
        assertThat(rank.position()).isNull();
        assertThat(rank.cohortSize()).isZero();
    }

    private Assessment assessment(Long id, Term assessmentTerm, Subject subject,
                                  Stream assessmentStream, String status, String maxScore) {
        return Assessment.builder()
                .id(id)
                .school(school)
                .classEntity(schoolClass)
                .stream(assessmentStream)
                .term(assessmentTerm)
                .subject(subject)
                .title("Assessment " + id)
                .type("TEST")
                .assessmentDate(LocalDate.of(2026, 5, 1))
                .maxScore(new BigDecimal(maxScore))
                .status(status)
                .build();
    }

    private Score score(Long id, Assessment assessment, String value) {
        return Score.builder().id(id).student(student).assessment(assessment)
                .score(new BigDecimal(value)).build();
    }
}
