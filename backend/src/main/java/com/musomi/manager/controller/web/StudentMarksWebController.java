package com.musomi.manager.controller.web;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Subject;
import com.musomi.manager.entity.Term;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.service.StudentPortalService;
import com.musomi.manager.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class StudentMarksWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final StudentPortalService studentPortalService;

    @GetMapping("/student/marks")
    public String showMarks(@AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }
        Term term = studentPortalService.currentTerm(student).orElse(null);
        Map<Long, SubjectMarkGroup> groups = new LinkedHashMap<>();
        for (Score score : studentPortalService.publishedScores(student, term)) {
            Assessment assessment = score.getAssessment();
            Subject subject = assessment.getSubject();
            if (subject == null || subject.getId() == null) {
                continue;
            }
            SubjectMarkGroup group = groups.computeIfAbsent(subject.getId(),
                    ignored -> new SubjectMarkGroup(subject.getId(), safe(subject.getName()).isBlank() ? "Unnamed subject" : safe(subject.getName())));
            group.marks.add(toMark(score));
        }

        model.addAttribute("pageTitle", "My Marks");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", student.getSchool() == null ? "" : student.getSchool().getName());
        model.addAttribute("termName", term == null ? "" : term.getName());
        model.addAttribute("subjectGroups", groups.values().stream().map(SubjectMarkGroup::toModel).toList());
        return "student/marks";
    }

    @GetMapping("/student/subjects/{subjectId}")
    public String showSubjectDetail(@PathVariable Long subjectId,
                                    @AuthenticationPrincipal WebUserPrincipal principal,
                                    Model model) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }
        Term term = studentPortalService.currentTerm(student).orElse(null);
        List<Score> scores = studentPortalService.publishedScores(student, term).stream()
                .filter(score -> score.getAssessment().getSubject() != null
                        && subjectId.equals(score.getAssessment().getSubject().getId()))
                .toList();
        if (scores.isEmpty()) {
            return "redirect:/student/marks";
        }

        Subject subject = scores.get(0).getAssessment().getSubject();
        List<Map<String, Object>> marks = scores.stream().map(this::toMark).toList();
        StudentPortalService.SubjectPerformance summary = studentPortalService.performance(student, term)
                .subjects().stream().filter(item -> subjectId.equals(item.subjectId()))
                .findFirst().orElse(null);
        String teacherName = scores.stream().map(score -> score.getAssessment().getTeacher())
                .filter(teacher -> teacher != null)
                .map(teacher -> safe(teacher.getFullName())).filter(name -> !name.isBlank())
                .findFirst().orElse("");

        model.addAttribute("pageTitle", "Subject Detail");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", student.getSchool() == null ? "" : student.getSchool().getName());
        model.addAttribute("subjectName", safe(subject.getName()));
        model.addAttribute("subjectCode", safe(subject.getCode()));
        model.addAttribute("teacherName", teacherName);
        model.addAttribute("average", summary == null ? null : summary.average());
        model.addAttribute("grade", summary == null ? null : summary.grade());
        model.addAttribute("marks", marks);
        return "student/subject-detail";
    }

    private Map<String, Object> toMark(Score score) {
        Assessment assessment = score.getAssessment();
        Map<String, Object> mark = new HashMap<>();
        mark.put("id", score.getId());
        mark.put("title", safe(assessment.getTitle()));
        mark.put("type", safe(assessment.getType()));
        mark.put("date", assessment.getAssessmentDate() == null
                ? "" : assessment.getAssessmentDate().format(DATE_FORMAT));
        mark.put("score", score.getScore());
        mark.put("maxScore", assessment.getMaxScore());
        mark.put("grade", GradeCalculator.calculate(score.getScore(), assessment.getMaxScore()));
        mark.put("topic", null);
        mark.put("feedback", score.getFeedback());
        return mark;
    }

    private String safe(String value) {
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
            model.put("marks", List.copyOf(marks));
            return model;
        }
    }
}
