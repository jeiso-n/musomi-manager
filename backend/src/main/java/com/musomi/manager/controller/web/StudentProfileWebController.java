package com.musomi.manager.controller.web;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.musomi.manager.entity.Guardian;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.User;
import com.musomi.manager.repository.GuardianRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.security.WebUserPrincipal;
import com.musomi.manager.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class StudentProfileWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final StudentPortalService studentPortalService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/student/profile")
    public String showProfile(@AuthenticationPrincipal WebUserPrincipal principal, Model model) {
        Student studentEntity = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (studentEntity == null) {
            return "redirect:/login";
        }
        List<Guardian> guardians = guardianRepository.findByStudentId(studentEntity.getId());
        Guardian guardianEntity = guardians.stream()
                .filter(guardian -> Boolean.TRUE.equals(guardian.getIsPrimary()))
                .findFirst().orElseGet(() -> guardians.stream().findFirst().orElse(null));

        String fullName = safe(studentEntity.getFullName());
        Map<String, Object> student = new HashMap<>();
        student.put("fullName", fullName);
        student.put("admissionNumber", safe(studentEntity.getAdmissionNumber()));
        student.put("className", studentEntity.getCurrentClass() == null ? "" : safe(studentEntity.getCurrentClass().getName()));
        student.put("streamName", studentEntity.getCurrentStream() == null ? "" : safe(studentEntity.getCurrentStream().getName()));
        student.put("gender", safe(studentEntity.getGender()));
        student.put("dob", studentEntity.getDateOfBirth() == null ? "" : studentEntity.getDateOfBirth().format(DATE_FORMAT));
        student.put("status", Boolean.TRUE.equals(studentEntity.getIsActive()) ? "Active" : "Inactive");

        Map<String, Object> guardian = new HashMap<>();
        guardian.put("name", guardianEntity == null ? "" : safe(guardianEntity.getName()));
        guardian.put("relationship", guardianEntity == null ? "" : safe(guardianEntity.getRelationship()));
        guardian.put("phone", guardianEntity == null ? "" : safe(guardianEntity.getPhone()));

        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("currentPage", "profile");
        model.addAttribute("schoolName", studentEntity.getSchool() == null ? "" : safe(studentEntity.getSchool().getName()));
        model.addAttribute("student", student);
        model.addAttribute("guardian", guardian);
        model.addAttribute("hasGuardian", guardianEntity != null);
        return "student/profile";
    }

    @PostMapping("/student/profile/change-password")
    @ResponseBody
    public String changePassword(
            @AuthenticationPrincipal WebUserPrincipal principal,
            @RequestParam(name = "current", required = false) String currentPassword,
            @RequestParam(name = "new", required = false) String newPassword,
            @RequestParam(name = "confirm", required = false) String confirmPassword,
            CsrfToken csrfToken) {
        Student student = studentPortalService.findCurrentStudent(principal).orElse(null);
        if (student == null) {
            return passwordResponse("Please sign in again before changing your password.", false, csrfToken);
        }
        Optional<User> userOpt = userRepository.findBySchoolIdAndUsername(
                principal.getSchoolId(), principal.getUsername());
        if (userOpt.isEmpty()) {
            return passwordResponse("Please sign in again before changing your password.", false, csrfToken);
        }
        User user = userOpt.get();
        if (newPassword == null || newPassword.length() < 8) {
            return passwordResponse("Password must be at least 8 characters.", false, csrfToken);
        }
        if (!newPassword.equals(confirmPassword)) {
            return passwordResponse("Passwords do not match.", false, csrfToken);
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            return passwordResponse("Enter your current password.", false, csrfToken);
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return passwordResponse("Invalid current password.", false, csrfToken);
        }
        try {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } catch (RuntimeException exception) {
            return passwordResponse("Unable to update your password. Please try again.", false, csrfToken);
        }
        return passwordResponse("Your password has been changed successfully.", true, csrfToken);
    }

    private String passwordResponse(String message, boolean success, CsrfToken csrfToken) {
        String color = success ? "emerald" : "red";
        String icon = success ? "check-circle" : "alert-circle";
        return "<form id=\"password-form\" hx-post=\"/student/profile/change-password\" hx-swap=\"outerHTML\" class=\"space-y-4\">"
                + "<input type=\"hidden\" name=\"" + csrfToken.getParameterName()
                + "\" value=\"" + csrfToken.getToken() + "\">"
                + "<div class=\"p-3.5 rounded-xl bg-" + color + "-50 border border-" + color + "-200 text-"
                + color + "-800 text-xs font-medium flex items-start gap-2.5 mb-3\">"
                + "<i data-lucide=\"" + icon + "\" class=\"w-4 h-4 text-" + color
                + "-600 shrink-0 mt-0.5\"></i><span>" + message + "</span></div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Current Password</label>"
                + "<input type=\"password\" name=\"current\" required autocomplete=\"current-password\" class=\"block w-full rounded-lg border border-slate-300 px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "<div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4\">"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">New Password</label>"
                + "<input type=\"password\" name=\"new\" required minlength=\"8\" autocomplete=\"new-password\" class=\"block w-full rounded-lg border border-slate-300 px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Confirm New Password</label>"
                + "<input type=\"password\" name=\"confirm\" required minlength=\"8\" autocomplete=\"new-password\" class=\"block w-full rounded-lg border border-slate-300 px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "</div><div class=\"pt-2\"><button type=\"submit\" class=\"w-full sm:w-auto inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700\">Update Password</button></div>"
                + "<script>lucide.createIcons();</script></form>";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
