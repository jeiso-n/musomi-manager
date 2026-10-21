package com.musomi.manager.controller.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.musomi.manager.entity.School;
import com.musomi.manager.entity.SchoolSettings;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.Term;
import com.musomi.manager.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for viewing and downloading student report card PDFs.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentReportsWebController {

    private final PdfService pdfService;

    @GetMapping("/student/reports")
    public String showReports(Model model) {
        model.addAttribute("pageTitle", "Report Cards");
        model.addAttribute("currentPage", "reports");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

        List<Map<String, Object>> reports = List.of(
                Map.of("id", "r1", "termName", "Term 1", "academicYear", "2026", "generatedAt", "18 Apr 2026", "fileSize", "148 KB", "downloadUrl", "/student/reports/download/r1"),
                Map.of("id", "r2", "termName", "Term 3", "academicYear", "2025", "generatedAt", "05 Dec 2025", "fileSize", "142 KB", "downloadUrl", "/student/reports/download/r2"),
                Map.of("id", "r3", "termName", "Term 2", "academicYear", "2025", "generatedAt", "12 Aug 2025", "fileSize", "139 KB", "downloadUrl", "/student/reports/download/r3")
        );
        model.addAttribute("reports", reports);

        return "student/reports";
    }

    @GetMapping("/student/reports/download/{id}")
    public ResponseEntity<byte[]> downloadReportCard(@PathVariable String id) {
        try {
            // Build report card PDF dynamically via OpenPDF PdfService
            School school = School.builder()
                    .name("St. Mary's Secondary School")
                    .address("Kisubi, Entebbe Road, Uganda")
                    .email("admin@stmarys.sc.ug")
                    .phone("+256 414 123456")
                    .build();

            Student student = Student.builder()
                    .admissionNumber("SMSS/2024/0142")
                    .fullName("Sarah Namubiru")
                    .school(school)
                    .build();

            Term term = Term.builder()
                    .name("Term 1")
                    .school(school)
                    .build();

            SchoolSettings settings = SchoolSettings.builder()
                    .school(school)
                    .reportHeader("St. Mary's Secondary School - Official Report Card")
                    .reportFooter("Promoted on Merit. School reopens next term on 25 May 2026.")
                    .gradingScale("{\"A\":80,\"B\":70,\"C\":60,\"D\":50,\"E\":40,\"F\":0}")
                    .build();

            List<PdfService.SubjectAverage> subjects = List.of(
                    new PdfService.SubjectAverage("Mathematics", BigDecimal.valueOf(84.5), "A", 3),
                    new PdfService.SubjectAverage("English Language", BigDecimal.valueOf(80.2), "A", 2),
                    new PdfService.SubjectAverage("Physics", BigDecimal.valueOf(78.8), "B", 2),
                    new PdfService.SubjectAverage("Biology", BigDecimal.valueOf(82.0), "A", 2),
                    new PdfService.SubjectAverage("Chemistry", BigDecimal.valueOf(80.0), "A", 2),
                    new PdfService.SubjectAverage("History & Political Ed.", BigDecimal.valueOf(88.5), "A", 2),
                    new PdfService.SubjectAverage("Geography", BigDecimal.valueOf(76.0), "B", 2)
            );

            byte[] pdfBytes = pdfService.generateReportCard(
                    student,
                    term,
                    settings,
                    subjects,
                    BigDecimal.valueOf(81.4),
                    "A",
                    4,
                    42,
                    List.of()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Report-Card-Sarah-Namubiru-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to generate PDF report card: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
