package com.musomi.manager.controller.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for student marks views and subject breakdowns.
 */
@Controller
@RequiredArgsConstructor
public class StudentMarksWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final TermRepository termRepository;

    @GetMapping("/student/marks")
    public String showMarks(
            @AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Optional<Student> studentOpt = findCurrentStudent(principal);
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student student = studentOpt.get();
        List<Score> scores = publishedScores(student);
        Map<Long, SubjectMarkGroup> groups = new LinkedHashMap<>();

        for (Score score : scores) {
            Assessment assessment = score.getAssessment();
            Subject subject = assessment.getSubject();
            if (subject == null || subject.getId() == null) {
                continue;
            }
            SubjectMarkGroup group = groups.computeIfAbsent(subject.getId(),
                    id -> new SubjectMarkGroup(id, valueOrEmpty(subject.getName())));
            group.marks.add(toMark(score));
        }
        List<Map<String, Object>> subjectGroups = groups.values().stream()
                .map(SubjectMarkGroup::toModel)
                .toList();

        model.addAttribute("pageTitle", "My Marks");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", student.getSchool() != null ? valueOrEmpty(student.getSchool().getName()) : "");
        model.addAttribute("termName", currentTermName(student));
        model.addAttribute("subjectGroups", subjectGroups);
        return "student/marks";
    }

    @GetMapping("/student/subjects/{subjectId}")
    public String showSubjectDetail(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal WebUserPrincipal principal,
            Model model) {
        Optional<Student> studentOpt = findCurrentStudent(principal);
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student student = studentOpt.get();
        List<Score> scores = publishedScores(student).stream()
                .filter(score -> score.getAssessment().getSubject() != null
                        && subjectId.equals(score.getAssessment().getSubject().getId()))
                .toList();
        if (scores.isEmpty()) {
            return "redirect:/student/marks";
        }

        Subject subject = scores.get(0).getAssessment().getSubject();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        List<Map<String, Object>> marks = new ArrayList<>();
        String teacherName = "";
        for (Score score : scores) {
            marks.add(toMark(score));
            BigDecimal maxScore = score.getAssessment().getMaxScore();
            if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                totalScore = totalScore.add(score.getScore());
                totalMax = totalMax.add(maxScore);
            }
            if (teacherName.isBlank() && score.getAssessment().getTeacher() != null) {
                teacherName = valueOrEmpty(score.getAssessment().getTeacher().getFullName());
            }
        }
        BigDecimal average = percentage(totalScore, totalMax);

        model.addAttribute("pageTitle", "Subject Detail");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", student.getSchool() != null ? valueOrEmpty(student.getSchool().getName()) : "");
        model.addAttribute("subjectName", valueOrEmpty(subject.getName()));
        model.addAttribute("subjectCode", valueOrEmpty(subject.getCode()));
        model.addAttribute("teacherName", teacherName);
        model.addAttribute("average", average);
        model.addAttribute("grade", GradeCalculator.calculate(totalScore, totalMax));
        model.addAttribute("marks", marks);
        return "student/subject-detail";
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

    private Map<String, Object> toMark(Score score) {
        Assessment assessment = score.getAssessment();
        Map<String, Object> mark = new HashMap<>();
        mark.put("id", score.getId());
        mark.put("title", valueOrEmpty(assessment.getTitle()));
        mark.put("type", valueOrEmpty(assessment.getType()));
        mark.put("date", assessment.getAssessmentDate() != null
                ? assessment.getAssessmentDate().format(DATE_FORMAT) : "");
        mark.put("score", score.getScore());
        mark.put("maxScore", assessment.getMaxScore() != null ? assessment.getMaxScore() : BigDecimal.ZERO);
        mark.put("grade", GradeCalculator.calculate(score.getScore(), assessment.getMaxScore()));
        mark.put("topic", null);
        mark.put("feedback", score.getFeedback());
        return mark;
    }

    private String currentTermName(Student student) {
        if (student.getSchool() == null) {
            return "";
        }
        return termRepository.findBySchoolIdAndIsCurrentTrue(student.getSchool().getId())
                .map(Term::getName)
                .orElse("");
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

    private BigDecimal percentage(BigDecimal score, BigDecimal maxScore) {
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 1, RoundingMode.HALF_UP);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class SubjectMarkGroup {
        private final Long subjectId;
        private final String subjectName;
        private final List<Map<String, Object>> marks = new ArrayList<>();

        private SubjectMarkGroup(Long subjectId, String subjectName) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
        }

        private Map<String, Object> toModel() {
            Map<String, Object> model = new HashMap<>();
            model.put("subjectId", subjectId);
            model.put("subjectName", subjectName);
            model.put("marks", marks);
            return model;
        }
    }
}
