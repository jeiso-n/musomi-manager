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
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.util.WebCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class StudentProfileWebController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/student/profile")
    public String showProfile(Model model) {
        Optional<User> userOpt = findCurrentUser();
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        Optional<Student> studentOpt = findStudent(userOpt.get());
        if (studentOpt.isEmpty()) {
            return "redirect:/login";
        }
        Student studentEntity = studentOpt.get();
        List<Guardian> guardians = guardianRepository.findByStudentId(studentEntity.getId());
        Guardian guardianEntity = guardians.stream()
                .filter(guardian -> Boolean.TRUE.equals(guardian.getIsPrimary()))
                .findFirst()
                .orElseGet(() -> guardians.stream().findFirst().orElse(null));
        String fullName = valueOrEmpty(studentEntity.getFullName());
        String firstName = fullName.isBlank() ? "" : fullName.trim().split("\\s+")[0];
        String className = studentEntity.getCurrentClass() != null
                ? valueOrEmpty(studentEntity.getCurrentClass().getName()) : "";
        String streamName = studentEntity.getCurrentStream() != null
                ? valueOrEmpty(studentEntity.getCurrentStream().getName()) : "";
        String gender = valueOrEmpty(studentEntity.getGender());
        String dateOfBirth = studentEntity.getDateOfBirth() != null
                ? studentEntity.getDateOfBirth().format(DATE_FORMAT) : "";
        String guardianName = guardianEntity != null ? valueOrEmpty(guardianEntity.getName()) : "";
        String guardianRelationship = guardianEntity != null ? valueOrEmpty(guardianEntity.getRelationship()) : "";
        String guardianPhone = guardianEntity != null ? valueOrEmpty(guardianEntity.getPhone()) : "";

        Map<String, Object> student = new HashMap<>();
        student.put("fullName", fullName);
        student.put("admissionNumber", valueOrEmpty(studentEntity.getAdmissionNumber()));
        student.put("className", className);
        student.put("streamName", streamName);
        student.put("gender", gender);
        student.put("dob", dateOfBirth);
        Map<String, Object> guardian = new HashMap<>();
        guardian.put("name", guardianName);
        guardian.put("relationship", guardianRelationship);
        guardian.put("phone", guardianPhone);

        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("currentPage", "profile");
        model.addAttribute("schoolName", studentEntity.getSchool() != null
                ? valueOrEmpty(studentEntity.getSchool().getName()) : "");
        model.addAttribute("studentEntity", studentEntity);
        model.addAttribute("student", student);
        model.addAttribute("firstName", firstName);
        model.addAttribute("fullName", fullName);
        model.addAttribute("admissionNumber", valueOrEmpty(studentEntity.getAdmissionNumber()));
        model.addAttribute("className", className);
        model.addAttribute("streamName", streamName);
        model.addAttribute("gender", gender);
        model.addAttribute("dateOfBirth", dateOfBirth);
        model.addAttribute("guardianEntity", guardianEntity);
        model.addAttribute("guardian", guardian);
        model.addAttribute("guardianName", guardianName);
        model.addAttribute("guardianPhone", guardianPhone);
        return "student/profile";
    }

    @PostMapping("/student/profile/change-password")
    @ResponseBody
    public String changePassword(
            @RequestParam(name = "current", required = false) String currentPassword,
            @RequestParam(name = "new", required = false) String newPassword,
            @RequestParam(name = "confirm", required = false) String confirmPassword) {
        Optional<User> userOpt = findCurrentUser();
        if (userOpt.isEmpty() || findStudent(userOpt.get()).isEmpty()) {
            return passwordResponse("Please sign in again before changing your password.", false);
        }
        User user = userOpt.get();
        if (newPassword == null || newPassword.length() < 8) {
            return passwordResponse("Password must be at least 8 characters.", false);
        }
        if (!newPassword.equals(confirmPassword)) {
            return passwordResponse("Passwords do not match.", false);
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            return passwordResponse("Enter your current password.", false);
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return passwordResponse("Invalid current password.", false);
        }
        try {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } catch (RuntimeException exception) {
            return passwordResponse("Unable to update your password. Please try again.", false);
        }
        return passwordResponse("Your password has been changed successfully.", true);
    }

    private Optional<User> findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return userRepository.findBySchoolIdAndUsername(WebCurrentUser.getSchoolId(), auth.getName());
    }

    private Optional<Student> findStudent(User user) {
        return studentRepository.findAll().stream()
                .filter(student -> student.getUser() != null
                        && user.getId().equals(student.getUser().getId()))
                .findFirst();
    }

    private String passwordResponse(String message, boolean success) {
        String color = success ? "emerald" : "red";
        String icon = success ? "check-circle" : "alert-circle";
        return "<form id=\"password-form\" hx-post=\"/student/profile/change-password\" hx-swap=\"outerHTML\" class=\"space-y-4\">"
                + "<div class=\"p-3.5 rounded-xl bg-" + color + "-50 border border-" + color + "-200 text-"
                + color + "-800 text-xs font-medium flex items-start gap-2.5 mb-3\">"
                + "<i data-lucide=\"" + icon + "\" class=\"w-4 h-4 text-" + color
                + "-600 shrink-0 mt-0.5\"></i><span>" + message + "</span></div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Current Password</label>"
                + "<input type=\"password\" name=\"current\" required class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "<div class=\"grid grid-cols-1 sm:grid-cols-2 gap-4\">"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">New Password</label>"
                + "<input type=\"password\" name=\"new\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "<div><label class=\"block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1\">Confirm New Password</label>"
                + "<input type=\"password\" name=\"confirm\" required minlength=\"6\" class=\"block w-full rounded-lg border border-slate-300 shadow-2xs px-3.5 py-2 text-sm text-slate-900\"></div>"
                + "</div><div class=\"pt-2\"><button type=\"submit\" class=\"w-full sm:w-auto inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700\">Update Password</button></div>"
                + "<script>lucide.createIcons();</script></form>";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
