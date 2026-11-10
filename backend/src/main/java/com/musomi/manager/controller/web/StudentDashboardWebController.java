package com.musomi.manager.controller.web;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.musomi.manager.entity.Assessment;
import com.musomi.manager.entity.Score;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import com.musomi.manager.service.StudentPortalService;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.util.GradeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StudentDashboardWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final StudentPortalService studentPortalService;

    @GetMapping("/student/dashboard")
    public String showDashboard(@AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        Term term = studentPortalService.currentTerm(student).orElse(null);
        StudentPortalService.TermPerformance performance = term == null
                ? StudentPortalService.TermPerformance.empty()
                : studentPortalService.performance(student, term);
        StudentPortalService.Rank rank = term == null
                ? new StudentPortalService.Rank(null, 0)
                : studentPortalService.rank(student, term, performance);

        List<Map<String, Object>> recentMarks = studentPortalService.publishedScores(student, term).stream()
                .limit(5)
                .map(this::toMark)
                .toList();

        String fullName = student.getFullName() == null ? "" : student.getFullName().trim();
        String firstName = fullName.isBlank() ? "" : fullName.split("\\s+")[0];
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("schoolName", student.getSchool() == null ? "" : student.getSchool().getName());
        model.addAttribute("firstName", firstName);
        model.addAttribute("fullName", fullName);
        model.addAttribute("admissionNumber", student.getAdmissionNumber());
        model.addAttribute("className", student.getCurrentClass() == null ? "" : student.getCurrentClass().getName());
        model.addAttribute("streamName", student.getCurrentStream() == null ? "" : student.getCurrentStream().getName());
        model.addAttribute("currentTerm", term == null ? "" : term.getName());
        model.addAttribute("currentYear", term == null || term.getAcademicYear() == null
                || term.getAcademicYear().getYear() == null ? "" : term.getAcademicYear().getYear().toString());
        model.addAttribute("average", performance.overallAverage());
        model.addAttribute("overallGrade", performance.overallGrade());
        model.addAttribute("position", rank.position());
        model.addAttribute("classSize", rank.cohortSize());
        model.addAttribute("reportComplete", performance.complete());
        model.addAttribute("expectedAssessments", performance.expectedAssessmentCount());
        model.addAttribute("recordedAssessments", performance.recordedAssessmentCount());
        model.addAttribute("recentMarks", recentMarks);
        return "student/dashboard";
    }

    private Map<String, Object> toMark(Score score) {
        Assessment assessment = score.getAssessment();
        Map<String, Object> mark = new HashMap<>();
        mark.put("subjectName", assessment.getSubject() == null ? "" : assessment.getSubject().getName());
        mark.put("title", assessment.getTitle());
        mark.put("type", assessment.getType());
        mark.put("date", assessment.getAssessmentDate() == null
                ? "" : assessment.getAssessmentDate().format(DATE_FORMAT));
        mark.put("score", score.getScore());
        mark.put("maxScore", assessment.getMaxScore());
        mark.put("grade", GradeCalculator.calculate(score.getScore(), assessment.getMaxScore()));
        return mark;
    }
}
