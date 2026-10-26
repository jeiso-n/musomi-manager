package com.musomi.manager.controller.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.util.GradeCalculator;
import com.musomi.manager.util.WebCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for term summary, subject averages, and class rankings.
 */
@Controller
@RequiredArgsConstructor
public class StudentResultsWebController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final TermRepository termRepository;

    @GetMapping("/student/results")
    public String showResults(Model model) {
        Optional<Student> studentOpt = findCurrentStudent();
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student student = studentOpt.get();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        Map<Long, SubjectTotals> totalsBySubject = new LinkedHashMap<>();

        for (Score score : publishedScores(student)) {
            Assessment assessment = score.getAssessment();
            BigDecimal maxScore = assessment.getMaxScore();
            Subject subject = assessment.getSubject();
            if (subject == null || subject.getId() == null || maxScore == null
                    || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            SubjectTotals totals = totalsBySubject.computeIfAbsent(subject.getId(),
                    ignored -> new SubjectTotals(subject));
            totals.score = totals.score.add(score.getScore());
            totals.maxScore = totals.maxScore.add(maxScore);
            totals.assessmentCount++;
            totalScore = totalScore.add(score.getScore());
            totalMax = totalMax.add(maxScore);
        }

        List<Map<String, Object>> subjects = new ArrayList<>();
        for (SubjectTotals totals : totalsBySubject.values()) {
            Map<String, Object> subject = new HashMap<>();
            subject.put("subjectId", totals.subject.getId());
            subject.put("subjectName", valueOrEmpty(totals.subject.getName()));
            subject.put("subjectCode", valueOrEmpty(totals.subject.getCode()));
            subject.put("average", percentage(totals.score, totals.maxScore));
            subject.put("grade", GradeCalculator.calculate(totals.score, totals.maxScore));
            subject.put("assessmentCount", totals.assessmentCount);
            subjects.add(subject);
        }

        int classSize = 0;
        int position = 0;
        if (student.getCurrentClass() != null) {
            List<Student> classmates = studentRepository.findByCurrentClassIdAndIsActiveTrue(
                    student.getCurrentClass().getId());
            classSize = classmates.size();
            Map<Long, BigDecimal> averages = new HashMap<>();
            for (Student classmate : classmates) {
                ScoreTotals classmateTotals = totalsForClassmate(classmate, student.getCurrentClass().getId());
                if (classmateTotals.maxScore.compareTo(BigDecimal.ZERO) > 0) {
                    averages.put(classmate.getId(), percentage(classmateTotals.score, classmateTotals.maxScore));
                }
            }
            BigDecimal ownAverage = averages.get(student.getId());
            if (ownAverage != null) {
                position = 1 + (int) averages.values().stream()
                        .filter(average -> average.compareTo(ownAverage) > 0)
                        .count();
            }
        }

        Term currentTerm = student.getSchool() == null ? null
                : termRepository.findBySchoolIdAndIsCurrentTrue(student.getSchool().getId()).orElse(null);
        model.addAttribute("pageTitle", "My Results");
        model.addAttribute("currentPage", "results");
        model.addAttribute("schoolName", student.getSchool() != null
                ? valueOrEmpty(student.getSchool().getName()) : "");
        model.addAttribute("termName", currentTerm != null ? valueOrEmpty(currentTerm.getName()) : "");
        model.addAttribute("year", currentTerm != null && currentTerm.getAcademicYear() != null
                && currentTerm.getAcademicYear().getYear() != null
                ? currentTerm.getAcademicYear().getYear().toString() : "");
        model.addAttribute("subjects", subjects);
        model.addAttribute("overallAverage", percentage(totalScore, totalMax));
        model.addAttribute("overallGrade", GradeCalculator.calculate(totalScore, totalMax));
        model.addAttribute("position", position);
        model.addAttribute("classSize", classSize);
        return "student/results";
    }

    private List<Score> publishedScores(Student student) {
        return scoreRepository.findByStudentId(student.getId()).stream()
                .filter(score -> score.getScore() != null && score.getAssessment() != null
                        && "PUBLISHED".equalsIgnoreCase(score.getAssessment().getStatus()))
                .toList();
    }

    private ScoreTotals totalsForClassmate(Student student, Long classId) {
        BigDecimal scoreTotal = BigDecimal.ZERO;
        BigDecimal maxTotal = BigDecimal.ZERO;
        for (Score score : publishedScores(student)) {
            Assessment assessment = score.getAssessment();
            BigDecimal maxScore = assessment.getMaxScore();
            if (assessment.getClassEntity() != null
                    && classId.equals(assessment.getClassEntity().getId())
                    && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                scoreTotal = scoreTotal.add(score.getScore());
                maxTotal = maxTotal.add(maxScore);
            }
        }
        return new ScoreTotals(scoreTotal, maxTotal);
    }

    private BigDecimal percentage(BigDecimal score, BigDecimal maxScore) {
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 1, RoundingMode.HALF_UP);
    }

    private Optional<Student> findCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findBySchoolIdAndUsername(WebCurrentUser.getSchoolId(), auth.getName());
        if (user.isEmpty()) {
            return Optional.empty();
        }
        Long userId = user.get().getId();
        return studentRepository.findAll().stream()
                .filter(student -> student.getUser() != null
                        && userId.equals(student.getUser().getId()))
                .findFirst();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class SubjectTotals {
        private final Subject subject;
        private BigDecimal score = BigDecimal.ZERO;
        private BigDecimal maxScore = BigDecimal.ZERO;
        private int assessmentCount;

        private SubjectTotals(Subject subject) {
            this.subject = subject;
        }
    }

    private record ScoreTotals(BigDecimal score, BigDecimal maxScore) {
    }
}
