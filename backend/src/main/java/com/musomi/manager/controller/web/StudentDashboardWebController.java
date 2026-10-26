package com.musomi.manager.controller.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.TermRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the student home dashboard.
 */
@Controller
@RequiredArgsConstructor
public class StudentDashboardWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final StudentRepository studentRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final TermRepository termRepository;

    @GetMapping("/student/dashboard")
    public String showDashboard(
            @AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Optional<Student> studentOpt = findCurrentStudent(principal);
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student student = studentOpt.get();
        String fullName = student.getFullName();
        List<Score> scores = publishedScores(student);
        List<Map<String, Object>> recentMarks = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;

        for (Score score : scores) {
            Assessment assessment = score.getAssessment();
            BigDecimal maxScore = assessment.getMaxScore();
            if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> mark = new HashMap<>();
            mark.put("id", score.getId());
            mark.put("subjectName", assessment.getSubject() != null
                    ? valueOrEmpty(assessment.getSubject().getName()) : "");
            mark.put("title", valueOrEmpty(assessment.getTitle()));
            mark.put("type", valueOrEmpty(assessment.getType()));
            mark.put("date", assessment.getAssessmentDate() != null
                    ? assessment.getAssessmentDate().format(DATE_FORMAT) : "");
            mark.put("score", score.getScore());
            mark.put("maxScore", maxScore);
            mark.put("grade", GradeCalculator.calculate(score.getScore(), maxScore));
            mark.put("topicName", null);
            recentMarks.add(mark);
            totalScore = totalScore.add(score.getScore());
            totalMax = totalMax.add(maxScore);
        }
        Term currentTerm = student.getSchool() == null ? null
                : termRepository.findBySchoolIdAndIsCurrentTrue(student.getSchool().getId()).orElse(null);
        int classSize = 0;
        int position = 0;
        if (student.getCurrentClass() != null) {
            List<Student> classmates = studentRepository.findByCurrentClassIdAndIsActiveTrue(
                    student.getCurrentClass().getId());
            classSize = classmates.size();
            position = classPosition(student, classmates);
        }

        String firstName = fullName == null || fullName.isBlank()
                ? "" : fullName.trim().split("\\s+")[0];
        BigDecimal average = percentage(totalScore, totalMax);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("schoolName", student.getSchool() != null
                ? valueOrEmpty(student.getSchool().getName()) : "");
        model.addAttribute("firstName", firstName);
        model.addAttribute("fullName", valueOrEmpty(fullName));
        model.addAttribute("admissionNumber", valueOrEmpty(student.getAdmissionNumber()));
        model.addAttribute("className", student.getCurrentClass() != null
                ? valueOrEmpty(student.getCurrentClass().getName()) : "");
        model.addAttribute("streamName", student.getCurrentStream() != null
                ? valueOrEmpty(student.getCurrentStream().getName()) : "");
        model.addAttribute("currentTerm", currentTerm != null ? valueOrEmpty(currentTerm.getName()) : "");
        model.addAttribute("currentYear", currentTerm != null && currentTerm.getAcademicYear() != null
                && currentTerm.getAcademicYear().getYear() != null
                ? currentTerm.getAcademicYear().getYear().toString() : "");
        model.addAttribute("average", average);
        model.addAttribute("overallGrade", GradeCalculator.calculate(totalScore, totalMax));
        model.addAttribute("position", position);
        model.addAttribute("classSize", classSize);
        model.addAttribute("recentMarks", recentMarks.stream().limit(5).toList());
        return "student/dashboard";
    }

    private int classPosition(Student student, List<Student> classmates) {
        BigDecimal ownAverage = null;
        List<BigDecimal> averages = new ArrayList<>();
        Long classId = student.getCurrentClass().getId();
        for (Student classmate : classmates) {
            BigDecimal scoreTotal = BigDecimal.ZERO;
            BigDecimal maxTotal = BigDecimal.ZERO;
            for (Score score : publishedScores(classmate)) {
                Assessment assessment = score.getAssessment();
                BigDecimal maxScore = assessment.getMaxScore();
                if (assessment.getClassEntity() != null
                        && classId.equals(assessment.getClassEntity().getId())
                        && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                    scoreTotal = scoreTotal.add(score.getScore());
                    maxTotal = maxTotal.add(maxScore);
                }
            }
            if (maxTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal average = percentage(scoreTotal, maxTotal);
                averages.add(average);
                if (classmate.getId().equals(student.getId())) {
                    ownAverage = average;
                }
            }
        }
        if (ownAverage == null) {
            return 0;
        }
        BigDecimal rankedAverage = ownAverage;
        return 1 + (int) averages.stream()
                .filter(average -> average.compareTo(rankedAverage) > 0)
                .count();
    }

    private List<Score> publishedScores(Student student) {
        return scoreRepository.findByStudentId(student.getId()).stream()
                .filter(score -> score.getScore() != null && score.getAssessment() != null
                        && "PUBLISHED".equalsIgnoreCase(score.getAssessment().getStatus()))
                .sorted(Comparator.comparing(
                        (Score score) -> score.getAssessment().getAssessmentDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private BigDecimal percentage(BigDecimal score, BigDecimal maxScore) {
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 1, RoundingMode.HALF_UP);
    }

    private Optional<Student> findCurrentStudent(WebUserPrincipal principal) {
        if (principal == null || principal.getSchoolId() == null
                || principal.getUsername() == null || principal.getUsername().isBlank()) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findBySchoolIdAndUsername(
                principal.getSchoolId(), principal.getUsername());
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
}
