package com.musomi.manager.controller.web;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for term summary, subject averages, and class rankings.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentResultsWebController {

    @GetMapping("/student/results")
    public String showResults(Model model) {
        model.addAttribute("pageTitle", "My Results");
        model.addAttribute("currentPage", "results");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

        model.addAttribute("termName", "Term 1");
        model.addAttribute("year", "2026");

        List<Map<String, Object>> subjects = List.of(
                Map.of("subjectId", "sub-math", "subjectName", "Mathematics", "subjectCode", "MTH", "average", 84.5, "grade", "A", "assessmentCount", 3),
                Map.of("subjectId", "sub-eng", "subjectName", "English Language", "subjectCode", "ENG", "average", 80.2, "grade", "A", "assessmentCount", 2),
                Map.of("subjectId", "sub-phy", "subjectName", "Physics", "subjectCode", "PHY", "average", 78.8, "grade", "B", "assessmentCount", 2),
                Map.of("subjectId", "sub-bio", "subjectName", "Biology", "subjectCode", "BIO", "average", 82.0, "grade", "A", "assessmentCount", 2),
                Map.of("subjectId", "sub-chem", "subjectName", "Chemistry", "subjectCode", "CHM", "average", 80.0, "grade", "A", "assessmentCount", 2),
                Map.of("subjectId", "sub-hist", "subjectName", "History & Political Ed.", "subjectCode", "HST", "average", 88.5, "grade", "A", "assessmentCount", 2),
                Map.of("subjectId", "sub-geo", "subjectName", "Geography", "subjectCode", "GEO", "average", 76.0, "grade", "B", "assessmentCount", 2),
                Map.of("subjectId", "sub-cre", "subjectName", "Christian Religious Ed.", "subjectCode", "CRE", "average", 85.0, "grade", "A", "assessmentCount", 1)
        );
        model.addAttribute("subjects", subjects);

        model.addAttribute("overallAverage", 81.9);
        model.addAttribute("overallGrade", "A");
        model.addAttribute("position", 4);
        model.addAttribute("classSize", 42);

        return "student/results";
    }
}
