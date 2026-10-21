package com.musomi.manager.controller.web;

import java.util.List;
import java.util.Map;

import com.musomi.manager.repository.AssessmentRepository;
import com.musomi.manager.repository.ScoreRepository;
import com.musomi.manager.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for student marks views and subject breakdowns.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentMarksWebController {

    private final SubjectRepository subjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;

    @GetMapping("/student/marks")
    public String showMarks(Model model) {
        model.addAttribute("pageTitle", "My Marks");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", "St. Mary's Secondary School");
        model.addAttribute("termName", "Term 1, 2026");

        // Rich subject groups conforming to MASTER.md & WEB.md specifications
        List<Map<String, Object>> mathMarks = List.of(
                Map.of("id", "m1", "title", "Mid-term Exam", "type", "TEST", "date", "15 Mar 2026", "score", 85.0, "maxScore", 100, "grade", "A", "topic", "Algebraic Expressions"),
                Map.of("id", "m2", "title", "Weekly Quiz: Quadratic Functions", "type", "QUIZ", "date", "22 Mar 2026", "score", 18.0, "maxScore", 20, "grade", "A", "topic", "Quadratic Equations"),
                Map.of("id", "m3", "title", "Continuous Assessment Assignment", "type", "HOMEWORK", "date", "05 Apr 2026", "score", 45.0, "maxScore", 50, "grade", "A", "topic", "Trigonometric Ratios")
        );

        List<Map<String, Object>> englishMarks = List.of(
                Map.of("id", "e1", "title", "Poetry Analysis Assignment", "type", "HOMEWORK", "date", "10 Mar 2026", "score", 78.5, "maxScore", 100, "grade", "B", "topic", "East African Poetry"),
                Map.of("id", "e2", "title", "Mid-term Summary & Comprehension", "type", "TEST", "date", "16 Mar 2026", "score", 82.0, "maxScore", 100, "grade", "A", "topic", "Reading Comprehension")
        );

        List<Map<String, Object>> physicsMarks = List.of(
                Map.of("id", "p1", "title", "Optics Practical Lab Work", "type", "PRACTICAL", "date", "12 Mar 2026", "score", 82.5, "maxScore", 100, "grade", "A", "topic", "Reflection & Refraction"),
                Map.of("id", "p2", "title", "Mechanics & Linear Motion Test", "type", "TEST", "date", "28 Mar 2026", "score", 75.0, "maxScore", 100, "grade", "B", "topic", "Newton's Laws of Motion")
        );

        List<Map<String, Object>> biologyMarks = List.of(
                Map.of("id", "b1", "title", "Cell Structure & Osmosis Quiz", "type", "QUIZ", "date", "02 Mar 2026", "score", 18.0, "maxScore", 20, "grade", "A", "topic", "Cell Organization"),
                Map.of("id", "b2", "title", "Plant Nutrition Mid-Term Test", "type", "TEST", "date", "20 Mar 2026", "score", 74.0, "maxScore", 100, "grade", "B", "topic", "Photosynthesis")
        );

        List<Map<String, Object>> chemistryMarks = List.of(
                Map.of("id", "c1", "title", "Periodic Table & Bonding Test", "type", "TEST", "date", "25 Feb 2026", "score", 74.0, "maxScore", 100, "grade", "B", "topic", "Ionic & Covalent Bonding"),
                Map.of("id", "c2", "title", "Acids, Bases & Salts Lab", "type", "PRACTICAL", "date", "18 Mar 2026", "score", 86.0, "maxScore", 100, "grade", "A", "topic", "Acid-Base Reactions")
        );

        List<Map<String, Object>> subjectGroups = List.of(
                Map.of("subjectId", "sub-math", "subjectName", "Mathematics", "marks", mathMarks),
                Map.of("subjectId", "sub-eng", "subjectName", "English Language", "marks", englishMarks),
                Map.of("subjectId", "sub-phy", "subjectName", "Physics", "marks", physicsMarks),
                Map.of("subjectId", "sub-bio", "subjectName", "Biology", "marks", biologyMarks),
                Map.of("subjectId", "sub-chem", "subjectName", "Chemistry", "marks", chemistryMarks)
        );

        model.addAttribute("subjectGroups", subjectGroups);
        return "student/marks";
    }

    @GetMapping("/student/subjects/{subjectId}")
    public String showSubjectDetail(@PathVariable String subjectId, Model model) {
        model.addAttribute("pageTitle", "Subject Detail");
        model.addAttribute("currentPage", "marks");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

        String subjectName = "Mathematics";
        String subjectCode = "MTH";
        String teacherName = "Mr. David Okello";
        String average = "84.5";
        String grade = "A";

        if (subjectId.contains("eng")) {
            subjectName = "English Language";
            subjectCode = "ENG";
            teacherName = "Ms. Grace Kyomugisha";
            average = "80.2";
            grade = "A";
        } else if (subjectId.contains("phy")) {
            subjectName = "Physics";
            subjectCode = "PHY";
            teacherName = "Mr. Ronald Kato";
            average = "78.8";
            grade = "B";
        } else if (subjectId.contains("bio")) {
            subjectName = "Biology";
            subjectCode = "BIO";
            teacherName = "Dr. Susan Nabirye";
            average = "82.0";
            grade = "A";
        } else if (subjectId.contains("chem")) {
            subjectName = "Chemistry";
            subjectCode = "CHM";
            teacherName = "Mr. Peter Mukasa";
            average = "80.0";
            grade = "A";
        }

        model.addAttribute("subjectName", subjectName);
        model.addAttribute("subjectCode", subjectCode);
        model.addAttribute("teacherName", teacherName);
        model.addAttribute("average", average);
        model.addAttribute("grade", grade);

        List<Map<String, Object>> marks = List.of(
                Map.of("id", "m1", "title", "Mid-term Examination", "type", "TEST", "date", "15 Mar 2026", "score", 85.0, "maxScore", 100, "grade", "A", "topic", "Core Theory", "feedback", "Great accuracy in solving step-by-step problems."),
                Map.of("id", "m2", "title", "Weekly Quiz: Topic Test", "type", "QUIZ", "date", "22 Mar 2026", "score", 18.0, "maxScore", 20, "grade", "A", "topic", "Unit Concepts", "feedback", "Very good recall of definitions."),
                Map.of("id", "m3", "title", "Continuous Assessment Assignment", "type", "HOMEWORK", "date", "05 Apr 2026", "score", 45.0, "maxScore", 50, "grade", "A", "topic", "Applied Practical Analysis", "feedback", "Neat work submitted on time.")
        );
        model.addAttribute("marks", marks);

        return "student/subject-detail";
    }
}
