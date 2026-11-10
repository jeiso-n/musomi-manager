package com.musomi.manager.controller.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.musomi.manager.entity.School;
import com.musomi.manager.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;

/** Handles the school-scoped student portal login screen. */
@Controller
@RequiredArgsConstructor
public class LoginWebController {

    private final SchoolRepository schoolRepository;

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(name = "error", required = false) String error,
            Model model) {
        List<School> schools = schoolRepository.findAll().stream()
                .sorted(Comparator.comparing(School::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        model.addAttribute("pageTitle", "Student Login");
        model.addAttribute("schoolName", "Musomi Manager");
        model.addAttribute("schools", schools);

        if (error != null) {
            String errorMessage = switch (error) {
                case "ACCOUNT_LOCKED" ->
                        "Too many failed attempts. Your account is locked. Please contact the school office.";
                case "ACCOUNT_INACTIVE" ->
                        "This account is currently inactive. Please contact the administration office.";
                case "SESSION_EXPIRED" -> "Your session expired. Please sign in again.";
                case "INVALID_CREDENTIALS" ->
                        "Wrong school, admission number or password. Check your details and try again.";
                default -> "Unable to sign in. Check your school, admission number and password.";
            };
            model.addAttribute("errorMessage", errorMessage);
        }
        return "student/login";
    }
}
