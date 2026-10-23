package com.musomi.manager.controller.web;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for student personal profile, guardian contact, and password management.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentProfileWebController {

    @GetMapping("/student/profile")
    public String showProfile(Model model) {
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("currentPage", "profile");
        model.addAttribute("schoolName", "St. Mary's Secondary School");

        Map<String, Object> student = Map.of(
                "fullName", "Sarah Namubiru",
                "admissionNumber", "SMSS/2024/0142",
                "className", "Senior 3",
                "streamName", "Blue",
                "gender", "Female",
                "dob", "12 May 2010"
        );
        model.addAttribute("student", student);

        Map<String, Object> guardian = Map.of(
                "name", "Dr. James Namubiru",
                "relationship", "Father",
                "phone", "+256 772 123 456"
        );
        model.addAttribute("guardian", guardian);

        return "student/profile";
    }

    @PostMapping("/student/profile/change-password")
    @ResponseBody
    public String changePassword(
            @RequestParam(name = "current", required = false) String current,
            @RequestParam(name = "new", required = false) String newPass,
            @RequestParam(name = "confirm", required = false) String confirm) {

        if (newPass == null || confirm == null || !newPass.equals(confirm)) {
            return "<form id=\"password-form\" hx-post=\"/student/profile/change-password\" hx-swap=\"outerHTML\" class=\"space-y-4\">"
                    + "<div class=\"p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-medium flex items-start gap-2.5 mb-3\">"
                    + "<i data-lucide=\"alert-circle\" class=\"w-4 h-4 text-red-600 shrink-0 mt-0.5\"></i>"
                    + "<span>New password and confirmation password do not match. Please try again.</span>"
                    + "</div>"
                    + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Current Password</label>"
                    + "<input type=\"password\" name=\"current\" required class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                    + "<div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4\">"
                    + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">New Password</label>"
                    + "<input type=\"password\" name=\"new\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                    + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Confirm New Password</label>"
                    + "<input type=\"password\" name=\"confirm\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                    + "</div>"
                    + "<div class=\"pt-2\"><button type=\"submit\" class=\"w-full sm:w-auto inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-sm focus:outline-none transition\">Update Password</button></div>"
                    + "<script>lucide.createIcons();</script>"
                    + "</form>";
        }

        return "<form id=\"password-form\" hx-post=\"/student/profile/change-password\" hx-swap=\"outerHTML\" class=\"space-y-4\">"
                + "<div class=\"p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium flex items-start gap-2.5 mb-3\">"
                + "<i data-lucide=\"check-circle\" class=\"w-4 h-4 text-emerald-600 shrink-0 mt-0.5\"></i>"
                + "<span>Your password has been changed successfully.</span>"
                + "</div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Current Password</label>"
                + "<input type=\"password\" name=\"current\" required class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                + "<div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4\">"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">New Password</label>"
                + "<input type=\"password\" name=\"new\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Confirm New Password</label>"
                + "<input type=\"password\" name=\"confirm\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:ring-1 focus:ring-indigo-600 transition\"></div>"
                + "</div>"
                + "<div class=\"pt-2\"><button type=\"submit\" class=\"w-full sm:w-auto inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-sm focus:outline-none transition\">Update Password</button></div>"
                + "<script>lucide.createIcons();</script>"
                + "</form>";
    }
}
