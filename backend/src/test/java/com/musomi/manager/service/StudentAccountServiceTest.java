package com.musomi.manager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.musomi.manager.entity.School;
import com.musomi.manager.entity.Student;
import com.musomi.manager.entity.User;
import com.musomi.manager.entity.enums.Role;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StudentAccountServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks private StudentAccountService service;

    @Test
    void provisionsLinkedStudentAccountsWithRandomOneTimeCredentials() {
        School school = School.builder().id(1L).build();
        Student student = Student.builder().id(10L).school(school)
                .admissionNumber("STU/001").fullName("Test Student")
                .status("ACTIVE").isActive(true).build();
        when(studentRepository.findBySchoolIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(student));
        when(userRepository.findBySchoolIdAndUsername(1L, "STU/001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.provisionForSchool(1L, 99L);

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.alreadyLinkedCount()).isZero();
        assertThat(result.skippedCount()).isZero();
        assertThat(result.createdAccounts()).singleElement().satisfies(credential -> {
            assertThat(credential.username()).isEqualTo("STU/001");
            assertThat(credential.temporaryPassword()).hasSize(12);
            assertThat(credential.temporaryPassword()).isNotEqualTo("encoded-password");
        });
        assertThat(student.getUser()).isNotNull();
        assertThat(student.getUser().getRole()).isEqualTo(Role.STUDENT);
        assertThat(student.getUser().getPasswordHash()).isEqualTo("encoded-password");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getUsername()).isEqualTo("STU/001");
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.STUDENT);
        verify(auditService).log("STUDENT_ACCOUNT_CREATED", "Student", 10L, 99L, 1L);
    }

    @Test
    void resetsAnExistingStudentPasswordAndReturnsOneTimeCredential() {
        School school = School.builder().id(1L).build();
        User existingUser = User.builder().id(20L).school(school)
                .username("STU/002").role(Role.STUDENT).isActive(true)
                .failedLoginAttempts(5).build();
        Student student = Student.builder().id(11L).school(school)
                .admissionNumber("STU/002").fullName("Existing Student")
                .status("ACTIVE").isActive(true).user(existingUser).build();
        when(studentRepository.findByIdAndSchoolId(11L, 1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(any(String.class))).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.resetStudentPassword(11L, 1L, 99L);

        assertThat(result.studentId()).isEqualTo(11L);
        assertThat(result.username()).isEqualTo("STU/002");
        assertThat(result.temporaryPassword()).hasSize(12);
        assertThat(existingUser.getPasswordHash()).isEqualTo("new-encoded-password");
        assertThat(existingUser.getFailedLoginAttempts()).isZero();
        assertThat(existingUser.getLockedUntil()).isNull();
        verify(auditService).log("STUDENT_PASSWORD_RESET", "Student", 11L, 99L, 1L);
    }

    @Test
    void doesNotResetPasswordForAnInactiveStudent() {
        Student student = Student.builder().id(13L).school(School.builder().id(1L).build())
                .admissionNumber("STU/003").fullName("Inactive Student")
                .status("INACTIVE").isActive(false)
                .user(User.builder().id(21L).role(Role.STUDENT).isActive(true).build())
                .build();
        when(studentRepository.findByIdAndSchoolId(13L, 1L)).thenReturn(Optional.of(student));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.resetStudentPassword(13L, 1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only active students");
    }

    @Test
    void doesNotRecreateAnAlreadyLinkedStudentAccount() {
        School school = School.builder().id(1L).build();
        User existingUser = User.builder().id(20L).school(school)
                .username("STU/002").role(Role.STUDENT).isActive(true).build();
        Student student = Student.builder().id(11L).school(school)
                .admissionNumber("STU/002").fullName("Already Linked")
                .status("ACTIVE").isActive(true).user(existingUser).build();
        when(studentRepository.findBySchoolIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(student));

        var result = service.provisionForSchool(1L, 99L);

        assertThat(result.createdCount()).isZero();
        assertThat(result.alreadyLinkedCount()).isEqualTo(1);
        assertThat(result.createdAccounts()).isEmpty();
    }

    @Test
    void skipsAdmissionNumberAlreadyOwnedByNonStudentAccount() {
        School school = School.builder().id(1L).build();
        Student student = Student.builder().id(12L).school(school)
                .admissionNumber("admin").fullName("Collision Student")
                .status("ACTIVE").isActive(true).build();
        User admin = User.builder().id(1L).school(school)
                .username("admin").role(Role.ADMIN).isActive(true).build();
        when(studentRepository.findBySchoolIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(student));
        when(userRepository.findBySchoolIdAndUsername(1L, "admin")).thenReturn(Optional.of(admin));

        var result = service.provisionForSchool(1L, 99L);

        assertThat(result.createdCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skippedAdmissions()).contains("admin (username already in use)");
        assertThat(student.getUser()).isNull();
    }
}
