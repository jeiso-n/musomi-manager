package com.musomi.manager.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.musomi.manager.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;

/**
 * Controller to handle web error pages.
 */
@Controller
@RequiredArgsConstructor
public class ErrorWebController implements ErrorController {

    private final SchoolRepository schoolRepository;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.addAttribute("pageTitle", "Error");
        addSchoolName(model);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "errors/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "errors/access-denied";
            }
        }
        return "errors/500";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied");
        addSchoolName(model);
        return "errors/access-denied";
    }

    @GetMapping("/500")
    public String serverError(Model model) {
        model.addAttribute("pageTitle", "Server Error");
        addSchoolName(model);
        return "errors/500";
    }

    private void addSchoolName(Model model) {
        model.addAttribute("schoolName", schoolRepository.findAll().stream()
                .findFirst().map(school -> school.getName()).orElse("Musomi Manager"));
    }
}
