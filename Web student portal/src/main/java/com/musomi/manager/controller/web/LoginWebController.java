package com.musomi.manager.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles web authentication screens for students.
 */
@Controller
public class LoginWebController {

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(name = "error", required = false) String error,
            Model model) {

        model.addAttribute("pageTitle", "Student Login");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

        if (error != null) {
            String errorMessage = "Wrong username or password.";
            switch (error) {
                case "ACCOUNT_LOCKED":
                    errorMessage = "Too many failed attempts. Your account is locked. Please contact the school office.";
                    break;
                case "ACCOUNT_INACTIVE":
                    errorMessage = "This account is currently inactive. Please contact the administration office.";
                    break;
                case "SESSION_EXPIRED":
                    errorMessage = "Your session expired. Please sign in again.";
                    break;
                case "INVALID_CREDENTIALS":
                default:
                    errorMessage = "Wrong username or password. Check your admission number and try again.";
                    break;
            }
            model.addAttribute("errorMessage", errorMessage);
        }

        return "student/login";
    }
}
