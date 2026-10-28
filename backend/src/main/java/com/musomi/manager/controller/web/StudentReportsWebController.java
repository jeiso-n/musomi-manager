package com.musomi.manager.controller.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.musomi.manager.entity.GeneratedReport;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.GeneratedReportRepository;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.UserRepository;
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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final GeneratedReportRepository generatedReportRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @GetMapping("/student/reports")
    public String showReports(Model model) {
        Optional<Student> studentOpt = findCurrentStudent();
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student student = studentOpt.get();
        List<Map<String, Object>> reports = new ArrayList<>();
        for (GeneratedReport report : generatedReportRepository
                .findByStudentIdOrderByGeneratedAtDesc(student.getId())) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("termName", report.getTerm() != null ? valueOrEmpty(report.getTerm().getName()) : "");
            item.put("academicYear", report.getTerm() != null
                    && report.getTerm().getAcademicYear() != null
                    && report.getTerm().getAcademicYear().getYear() != null
                    ? report.getTerm().getAcademicYear().getYear().toString() : "");
            item.put("generatedAt", report.getGeneratedAt() != null
                    ? report.getGeneratedAt().format(DATE_FORMAT) : "");
            item.put("fileSize", formatFileSize(report.getFileSizeBytes()));
            item.put("downloadUrl", "/student/reports/download/" + report.getId());
            reports.add(item);
        }
        model.addAttribute("pageTitle", "Report Cards");
        model.addAttribute("currentPage", "reports");
        model.addAttribute("schoolName", student.getSchool() != null
                ? valueOrEmpty(student.getSchool().getName()) : "");
        model.addAttribute("reports", reports);
        return "student/reports";
    }

    @GetMapping("/student/reports/download/{id}")
    public ResponseEntity<byte[]> downloadReportCard(@PathVariable Long id) {
        Optional<Student> studentOpt = findCurrentStudent();
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        Optional<GeneratedReport> reportOpt = generatedReportRepository
                .findByStudentIdOrderByGeneratedAtDesc(studentOpt.get().getId()).stream()
                .filter(report -> id.equals(report.getId()))
                .findFirst();
        if (reportOpt.isEmpty() || reportOpt.get().getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path reportPath = Path.of(reportOpt.get().getFilePath()).normalize();
        if (!Files.isRegularFile(reportPath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdfBytes = Files.readAllBytes(reportPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Report-Card-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IOException exception) {
            log.error("Failed to read generated report {} for student {}", id, studentOpt.get().getId(), exception);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Optional<Student> findCurrentStudent() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findBySchoolIdAndUsername(1L, auth.getName());
        if (user.isEmpty()) {
            return Optional.empty();
        }
        Long userId = user.get().getId();
        return studentRepository.findAll().stream()
                .filter(student -> student.getUser() != null
                        && userId.equals(student.getUser().getId()))
                .findFirst();
    }

    private String formatFileSize(Long fileSizeBytes) {
        return fileSizeBytes == null ? "" : String.format("%.0f KB", fileSizeBytes / 1024.0);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
