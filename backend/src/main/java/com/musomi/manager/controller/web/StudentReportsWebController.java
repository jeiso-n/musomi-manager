package com.musomi.manager.controller.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.musomi.manager.entity.GeneratedReport;
import com.musomi.manager.entity.Student;
import com.musomi.manager.repository.GeneratedReportRepository;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentReportsWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final GeneratedReportRepository generatedReportRepository;
    private final StudentPortalService studentPortalService;

    @GetMapping("/student/reports")
    public String showReports(@AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }
        List<Map<String, Object>> reports = new ArrayList<>();
        for (GeneratedReport report : generatedReportRepository
                .findByStudentIdOrderByGeneratedAtDesc(student.getId())) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("termName", report.getTerm() == null ? "" : safe(report.getTerm().getName()));
            item.put("academicYear", report.getTerm() == null || report.getTerm().getAcademicYear() == null
                    || report.getTerm().getAcademicYear().getYear() == null
                    ? "" : report.getTerm().getAcademicYear().getYear().toString());
            item.put("generatedAt", report.getGeneratedAt() == null
                    ? "" : report.getGeneratedAt().format(DATE_FORMAT));
            item.put("fileSize", formatFileSize(report.getFileSizeBytes()));
            item.put("downloadUrl", "/student/reports/download/" + report.getId());
            reports.add(item);
        }
        model.addAttribute("pageTitle", "Report Cards");
        model.addAttribute("currentPage", "reports");
        model.addAttribute("schoolName", student.getSchool() == null ? "" : safe(student.getSchool().getName()));
        model.addAttribute("reports", reports);
        return "student/reports";
    }

    @GetMapping("/student/reports/download/{id}")
    public ResponseEntity<byte[]> downloadReportCard(
            @PathVariable Long id, @AuthenticationPrincipal WebUserPrincipal principal) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        GeneratedReport report = generatedReportRepository
                .findByStudentIdOrderByGeneratedAtDesc(student.getId()).stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst().orElse(null);
        if (report == null || report.getFilePath() == null || report.getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path reportPath = Path.of(report.getFilePath()).normalize();
            if (!Files.isRegularFile(reportPath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdfBytes = Files.readAllBytes(reportPath);
            if (pdfBytes.length < 5 || pdfBytes[0] != '%'
                    || pdfBytes[1] != 'P' || pdfBytes[2] != 'D'
                    || pdfBytes[3] != 'F' || pdfBytes[4] != '-') {
                log.error("Stored report {} is not a valid PDF", id);
                return ResponseEntity.internalServerError().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Report-Card-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IOException | RuntimeException exception) {
            log.error("Failed to read generated report {} for student {}", id, student.getId(), exception);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String formatFileSize(Long bytes) {
        return bytes == null ? "" : String.format("%.0f KB", bytes / 1024.0);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
