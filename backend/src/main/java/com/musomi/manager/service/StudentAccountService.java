package com.musomi.manager.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.musomi.manager.dto.response.StudentAccountCredentialResponse;
import com.musomi.manager.dto.response.StudentAccountProvisioningResponse;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.User;
import com.musomi.manager.entity.enums.Role;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions linked student portal accounts and returns temporary credentials once.
 * Plaintext passwords are never persisted or logged.
 */
@Service
@RequiredArgsConstructor
public class StudentAccountService {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public StudentAccountProvisioningResponse provisionForSchool(Long schoolId, Long adminUserId) {
        if (schoolId == null || schoolId <= 0 || adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("A valid school and administrator are required.");
        }

        List<StudentAccountCredentialResponse> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int alreadyLinked = 0;

        List<Student> students = studentRepository.findBySchoolIdAndStatus(schoolId, "ACTIVE").stream()
                .filter(student -> Boolean.TRUE.equals(student.getIsActive()))
                .sorted(java.util.Comparator.comparing(Student::getAdmissionNumber,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        for (Student student : students) {
            if (student.getUser() != null) {
                if (student.getUser().getRole() == Role.STUDENT
                        && Boolean.TRUE.equals(student.getUser().getIsActive())) {
                    alreadyLinked++;
                } else {
                    skipped.add(student.getAdmissionNumber() + " (linked account is not an active student account)");
                }
                continue;
            }
            String username = student.getAdmissionNumber() == null
                    ? "" : student.getAdmissionNumber().trim();
            if (username.isBlank()) {
                skipped.add(student.getAdmissionNumber() + " (missing admission number)");
                continue;
            }

            User user = userRepository.findBySchoolIdAndUsername(schoolId, username).orElse(null);
            if (user != null) {
                boolean alreadyAssigned = studentRepository
                        .findFirstBySchoolIdAndUserId(schoolId, user.getId()).isPresent();
                if (user.getRole() != Role.STUDENT || alreadyAssigned) {
                    skipped.add(username + " (username already in use)");
                    continue;
                }
            } else {
                user = User.builder()
                        .school(student.getSchool())
                        .username(username)
                        .fullName(student.getFullName())
                        .role(Role.STUDENT)
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
            }

            String temporaryPassword = generateTemporaryPassword();
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setIsActive(true);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            User savedUser = userRepository.save(user);

            student.setUser(savedUser);
            student.setUpdatedAt(java.time.LocalDateTime.now());
            studentRepository.save(student);
            auditService.log("STUDENT_ACCOUNT_CREATED", "Student", student.getId(), adminUserId, schoolId);

            created.add(new StudentAccountCredentialResponse(
                    student.getId(), student.getFullName(), student.getAdmissionNumber(),
                    username, temporaryPassword));
        }

        return new StudentAccountProvisioningResponse(
                created.size(), alreadyLinked, skipped.size(), List.copyOf(created), List.copyOf(skipped));
    }

    /**
     * Sets a permanent password on the student's one canonical User account.
     * The same hash is used for student portal login and self-service changes.
     */
    @Transactional
    public void setStudentPassword(Long studentId, Long schoolId, Long adminUserId, String newPassword) {
        if (studentId == null || studentId <= 0 || schoolId == null || schoolId <= 0
                || adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("A valid student, school and administrator are required.");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.length() > 128) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters.");
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Student was not found in this school."));
        if (!Boolean.TRUE.equals(student.getIsActive()) || !"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new IllegalArgumentException("Only active students can have their portal password changed.");
        }
        User user = student.getUser();
        if (user == null || user.getRole() != Role.STUDENT
                || !Boolean.TRUE.equals(user.getIsActive())
                || user.getSchool() == null || !schoolId.equals(user.getSchool().getId())) {
            throw new IllegalArgumentException("This student does not have an active linked student login.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditService.log("STUDENT_PASSWORD_CHANGED_BY_ADMIN", "Student", student.getId(), adminUserId, schoolId);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int index = 0; index < TEMP_PASSWORD_LENGTH; index++) {
            password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}
