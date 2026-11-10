package com.musomi.manager.controller.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import com.musomi.manager.service.StudentPortalService;
import com.musomi.manager.security.WebUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StudentResultsWebController {

    private final StudentPortalService studentPortalService;

    @GetMapping("/student/results")
    public String showResults(@AuthenticationPrincipal WebUserPrincipal principal, Model model) {
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

        List<Map<String, Object>> subjects = new ArrayList<>();
        for (StudentPortalService.SubjectPerformance subject : performance.subjects()) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectId", subject.subjectId());
            item.put("subjectName", subject.subjectName());
            item.put("subjectCode", subject.subjectCode());
            item.put("average", subject.average());
            item.put("grade", subject.grade());
            item.put("assessmentCount", subject.assessmentCount());
            item.put("expectedAssessmentCount", subject.expectedAssessmentCount());
            item.put("missingAssessmentCount",
                    subject.expectedAssessmentCount() - subject.assessmentCount());
            subjects.add(item);
        }

        model.addAttribute("pageTitle", "My Results");
        model.addAttribute("currentPage", "results");
        model.addAttribute("schoolName", student.getSchool() == null ? "" : student.getSchool().getName());
        model.addAttribute("termName", term == null ? "" : term.getName());
        model.addAttribute("year", term == null || term.getAcademicYear() == null
                || term.getAcademicYear().getYear() == null ? "" : term.getAcademicYear().getYear().toString());
        model.addAttribute("subjects", subjects);
        model.addAttribute("overallAverage", performance.overallAverage());
        model.addAttribute("overallGrade", performance.overallGrade());
        model.addAttribute("position", rank.position());
        model.addAttribute("classSize", rank.cohortSize());
        model.addAttribute("expectedAssessments", performance.expectedAssessmentCount());
        model.addAttribute("recordedAssessments", performance.recordedAssessmentCount());
        model.addAttribute("complete", performance.complete());
        return "student/results";
    }
}
