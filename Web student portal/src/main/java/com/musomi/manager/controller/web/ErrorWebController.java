package com.musomi.manager.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle web error pages.
 */
@Controller
public class ErrorWebController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.addAttribute("pageTitle", "Error");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

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
        model.addAttribute("schoolName", "St. Mary's Secondary School");
        return "errors/access-denied";
    }

    @GetMapping("/500")
    public String serverError(Model model) {
        model.addAttribute("pageTitle", "Server Error");
        model.addAttribute("schoolName", "St. Mary's Secondary School");
        return "errors/500";
    }
}
