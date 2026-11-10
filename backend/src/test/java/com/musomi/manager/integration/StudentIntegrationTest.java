package com.musomi.manager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musomi.manager.AbstractIntegrationTest;
import com.musomi.manager.entity.User;
import com.musomi.manager.entity.Student;
import com.musomi.manager.repository.StudentRepository;
import com.musomi.manager.repository.UserRepository;
import com.musomi.manager.security.WebUserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(roles = "ADMIN")
@Import(StudentIntegrationTest.JsonConfiguration.class)
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration(proxyBeanMethods = false)
    static class JsonConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    @DisplayName("should create and retrieve student")
    void shouldCreateAndRetrieveStudent() throws Exception {
        JsonNode created = createStudent(uniqueAdmission("A"), "Integration Test Student");
        long id = created.path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/students/{id}", id)
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Integration Test Student"));
    }

    @Test
    @DisplayName("should throw when admission number is duplicated")
    void shouldThrowWhenDuplicateAdmissionNumber() throws Exception {
        String admissionNumber = uniqueAdmission("B");
        createStudent(admissionNumber, "First Duplicate Test");

        mockMvc.perform(post("/api/v1/admin/students")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentRequest(admissionNumber, "Second Duplicate Test")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ADMISSION_NUMBER"));
    }

    @Test
    @DisplayName("should update student details")
    void shouldUpdateStudentDetails() throws Exception {
        JsonNode created = createStudent(uniqueAdmission("C"), "Before Update");
        long id = created.path("data").path("id").asLong();
        String request = objectMapper.writeValueAsString(Map.of(
                "fullName", "After Update",
                "gender", "FEMALE",
                "currentClassId", 1,
                "currentStreamId", 1,
                "status", "ACTIVE"));

        mockMvc.perform(put("/api/v1/admin/students/{id}", id)
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("After Update"));
    }

    @Test
    @DisplayName("should list students with class filter")
    void shouldListStudentsWithClassFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L))
                        .param("classId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("should deactivate student")
    void shouldDeactivateStudent() throws Exception {
        JsonNode created = createStudent(uniqueAdmission("D"), "Student To Deactivate");
        long id = created.path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/admin/students/{id}", id)
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/students/{id}", id)
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("should return 404 when student is not found")
    void shouldReturn404WhenStudentNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students/999999")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should hide student when admin belongs to another school")
    void shouldHideStudentFromAnotherSchool() throws Exception {
        JsonNode created = createStudent(uniqueAdmission("E"), "School Scoped Student");
        long id = created.path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/students/{id}", id)
                        .with(withAuthenticatedUser("ADMIN", 1L, 999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should provision a secure linked portal login for an active student")
    void shouldProvisionStudentPortalAccount() throws Exception {
        String admissionNumber = uniqueAdmission("PORTAL");
        JsonNode created = createStudent(admissionNumber, "Portal Account Student");
        long studentId = created.path("data").path("id").asLong();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/students/provision-accounts")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCount").isNumber())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode credential = null;
        for (JsonNode item : response.path("data").path("createdAccounts")) {
            if (admissionNumber.equals(item.path("admissionNumber").asText())) {
                credential = item;
                break;
            }
        }
        assertThat(credential).isNotNull();
        assertThat(credential.path("username").asText()).isEqualTo(admissionNumber);
        assertThat(credential.path("temporaryPassword").asText()).hasSize(12);

        mockMvc.perform(formLogin().user("1::" + admissionNumber)
                        .password(credential.path("temporaryPassword").asText()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/student/dashboard"));

        User linkedUser = userRepository.findBySchoolIdAndUsername(1L, admissionNumber).orElseThrow();
        assertThat(linkedUser.getRole().name()).isEqualTo("STUDENT");
        assertThat(studentRepository.findFirstBySchoolIdAndUserId(1L, linkedUser.getId()))
                .isPresent().get().extracting(student -> student.getId()).isEqualTo(studentId);
    }
    @Test
    @DisplayName("should render student portal pages using the authenticated student's own account")
    void shouldRenderStudentPortalPages() throws Exception {
        String admissionNumber = uniqueAdmission("WEB");
        createStudent(admissionNumber, "Portal Page Student");
        mockMvc.perform(post("/api/v1/admin/students/provision-accounts")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L)))
                .andExpect(status().isOk());
        User studentUser = userRepository.findBySchoolIdAndUsername(1L, admissionNumber).orElseThrow();
        WebUserPrincipal principal = new WebUserPrincipal(studentUser.getUsername(), studentUser.getPasswordHash(),
                true, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")), 1L);

        for (String path : List.of("/student/dashboard", "/student/marks", "/student/results",
                "/student/reports", "/student/profile")) {
            mockMvc.perform(get(path).with(user(principal)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/student/profile").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }
    private JsonNode createStudent(String admissionNumber, String fullName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/students")
                        .with(withAuthenticatedUser("ADMIN", 1L, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentRequest(admissionNumber, fullName)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("data").path("id").asLong()).isPositive();
        return response;
    }

    private String studentRequest(String admissionNumber, String fullName) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "admissionNumber", admissionNumber,
                "fullName", fullName,
                "gender", "FEMALE",
                "currentClassId", 1,
                "currentStreamId", 1));
    }

    private String uniqueAdmission(String suffix) {
        return "INT/" + System.currentTimeMillis() + "/" + suffix + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }
}
