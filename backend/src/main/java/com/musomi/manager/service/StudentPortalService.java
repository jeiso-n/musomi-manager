package com.musomi.manager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.AssessmentRepository;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared, school-scoped student portal queries and term result calculations.
 * Only published assessments for the student's current class, stream and term
 * are considered. Missing scores are never converted to zero.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortalService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;

    public Optional<Student> findCurrentStudent(WebUserPrincipal principal) {
        if (principal == null || principal.getSchoolId() == null
                || principal.getUsername() == null || principal.getUsername().isBlank()) {
            return Optional.empty();
        }
        return userRepository.findBySchoolIdAndUsername(principal.getSchoolId(), principal.getUsername())
                .flatMap(user -> studentRepository.findFirstBySchoolIdAndUserId(
                        principal.getSchoolId(), user.getId()))
                .filter(student -> Boolean.TRUE.equals(student.getIsActive()))
                .filter(student -> "ACTIVE".equalsIgnoreCase(student.getStatus()));
    }

    public Optional<Student> findCurrentStudent(Long schoolId, String username) {
        if (schoolId == null || username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findBySchoolIdAndUsername(schoolId, username)
                .flatMap(user -> studentRepository.findFirstBySchoolIdAndUserId(schoolId, user.getId()))
                .filter(student -> Boolean.TRUE.equals(student.getIsActive()))
                .filter(student -> "ACTIVE".equalsIgnoreCase(student.getStatus()));
    }

    public Optional<Term> currentTerm(Student student) {
        if (student == null || student.getSchool() == null || student.getSchool().getId() == null) {
            return Optional.empty();
        }
        return termRepository.findBySchoolIdAndIsCurrentTrue(student.getSchool().getId());
    }

    public List<Assessment> publishedAssessments(Student student, Term term) {
        if (student == null || term == null || student.getSchool() == null
                || student.getCurrentClass() == null) {
            return List.of();
        }
        Long schoolId = student.getSchool().getId();
        Long classId = student.getCurrentClass().getId();
        Long termId = term.getId();
        Long streamId = student.getCurrentStream() == null ? null : student.getCurrentStream().getId();
        return assessmentRepository.findByClassEntityIdAndTermId(classId, termId).stream()
                .filter(assessment -> assessment.getSchool() != null
                        && schoolId.equals(assessment.getSchool().getId()))
                .filter(assessment -> assessment.getClassEntity() != null
                        && classId.equals(assessment.getClassEntity().getId()))
                .filter(assessment -> assessment.getTerm() != null
                        && termId.equals(assessment.getTerm().getId()))
                .filter(assessment -> "PUBLISHED".equalsIgnoreCase(assessment.getStatus()))
                .filter(assessment -> assessment.getStream() == null
                        || streamId != null && streamId.equals(assessment.getStream().getId()))
                .sorted(Comparator.comparing(Assessment::getAssessmentDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<Score> publishedScores(Student student, Term term) {
        List<Long> assessmentIds = publishedAssessments(student, term).stream()
                .map(Assessment::getId).filter(java.util.Objects::nonNull).toList();
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return scoreRepository.findByStudentId(student.getId()).stream()
                .filter(score -> score.getScore() != null && score.getAssessment() != null
                        && assessmentIds.contains(score.getAssessment().getId()))
                .sorted(Comparator.comparing(
                        (Score score) -> score.getAssessment().getAssessmentDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
    public TermPerformance performance(Student student, Term term) {
        if (student == null || term == null || student.getSchool() == null
                || student.getCurrentClass() == null || student.getId() == null) {
            return TermPerformance.empty();
        }

        List<Assessment> assessments = publishedAssessments(student, term);
        Map<Long, Score> scoresByAssessment = new HashMap<>();
        for (Score score : scoreRepository.findByStudentId(student.getId())) {
            if (score.getAssessment() != null && score.getAssessment().getId() != null
                    && score.getScore() != null) {
                scoresByAssessment.put(score.getAssessment().getId(), score);
            }
        }

        Map<Long, SubjectTotals> totalsBySubject = new java.util.LinkedHashMap<>();
        int recorded = 0;
        for (Assessment assessment : assessments) {
            Subject subject = assessment.getSubject();
            if (subject == null || subject.getId() == null) {
                continue;
            }
            SubjectTotals totals = totalsBySubject.computeIfAbsent(subject.getId(),
                    ignored -> new SubjectTotals(subject));
            totals.expected++;
            Score score = assessment.getId() == null ? null : scoresByAssessment.get(assessment.getId());
            BigDecimal maxScore = assessment.getMaxScore();
            if (score == null || maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal percentage = score.getScore().multiply(BigDecimal.valueOf(100))
                    .divide(maxScore, 4, RoundingMode.HALF_UP);
            totals.total = totals.total.add(percentage);
            totals.recorded++;
            recorded++;
        }

        List<SubjectPerformance> subjects = new ArrayList<>();
        for (SubjectTotals totals : totalsBySubject.values()) {
            BigDecimal average = totals.recorded == 0 ? null
                    : totals.total.divide(BigDecimal.valueOf(totals.recorded), 1, RoundingMode.HALF_UP);
            subjects.add(new SubjectPerformance(
                    totals.subject.getId(), safe(totals.subject.getName()), safe(totals.subject.getCode()),
                    average, GradeCalculator.calculate(average, BigDecimal.valueOf(100)),
                    totals.recorded, totals.expected));
        }

        List<BigDecimal> subjectAverages = subjects.stream()
                .map(SubjectPerformance::average).filter(value -> value != null).toList();
        BigDecimal overallAverage = subjectAverages.isEmpty() ? null
                : subjectAverages.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(subjectAverages.size()), 1, RoundingMode.HALF_UP);
        int expected = assessments.size();
        boolean complete = expected > 0 && recorded == expected;
        return new TermPerformance(List.copyOf(subjects), overallAverage,
                GradeCalculator.calculate(overallAverage, BigDecimal.valueOf(100)),
                expected, recorded, complete);
    }

    public Rank rank(Student student, Term term, TermPerformance ownPerformance) {
        if (student == null || term == null || ownPerformance == null
                || !ownPerformance.complete() || ownPerformance.overallAverage() == null
                || student.getCurrentClass() == null || student.getSchool() == null) {
            return new Rank(null, 0);
        }
        Long streamId = student.getCurrentStream() == null ? null : student.getCurrentStream().getId();
        List<Student> cohort = studentRepository
                .findByCurrentClassIdAndIsActiveTrue(student.getCurrentClass().getId()).stream()
                .filter(candidate -> "ACTIVE".equalsIgnoreCase(candidate.getStatus()))
                .filter(candidate -> candidate.getSchool() != null
                        && student.getSchool().getId().equals(candidate.getSchool().getId()))
                .filter(candidate -> {
                    Long candidateStream = candidate.getCurrentStream() == null
                            ? null : candidate.getCurrentStream().getId();
                    return java.util.Objects.equals(streamId, candidateStream);
                })
                .toList();

        List<BigDecimal> completeAverages = cohort.stream()
                .map(candidate -> performance(candidate, term))
                .filter(TermPerformance::complete)
                .map(TermPerformance::overallAverage)
                .filter(value -> value != null)
                .toList();
        long ahead = completeAverages.stream()
                .filter(value -> value.compareTo(ownPerformance.overallAverage()) > 0).count();
        return new Rank((int) ahead + 1, completeAverages.size());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class SubjectTotals {
        private final Subject subject;
        private BigDecimal total = BigDecimal.ZERO;
        private int recorded;
        private int expected;

        private SubjectTotals(Subject subject) {
            this.subject = subject;
        }
    }

    public record SubjectPerformance(Long subjectId, String subjectName, String subjectCode,
                                     BigDecimal average, String grade,
                                     int assessmentCount, int expectedAssessmentCount) {
    }

    public record TermPerformance(List<SubjectPerformance> subjects, BigDecimal overallAverage,
                                  String overallGrade, int expectedAssessmentCount,
                                  int recordedAssessmentCount, boolean complete) {
        public static TermPerformance empty() {
            return new TermPerformance(List.of(), null, null, 0, 0, false);
        }
    }

    public record Rank(Integer position, int cohortSize) {
    }
}

