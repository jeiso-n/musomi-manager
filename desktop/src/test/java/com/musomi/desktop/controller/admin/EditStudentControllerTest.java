package com.musomi.desktop.controller.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.musomi.desktop.api.AdminStudentApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditStudentControllerTest {

    @Test
    @DisplayName("AdminStudentApi.normalizeStatus maps valid, lowercase, whitespace, and null statuses correctly")
    void normalizeStatus_convertsValidAndNullStatuses() {
        assertEquals("ACTIVE", AdminStudentApi.normalizeStatus("ACTIVE"));
        assertEquals("ACTIVE", AdminStudentApi.normalizeStatus("Active"));
        assertEquals("ACTIVE", AdminStudentApi.normalizeStatus("  active  "));
        assertEquals("INACTIVE", AdminStudentApi.normalizeStatus("Inactive"));
        assertEquals("INACTIVE", AdminStudentApi.normalizeStatus("inactive"));
        assertEquals("DROPPED_OUT", AdminStudentApi.normalizeStatus("Dropped Out"));
        assertEquals("DROPPED_OUT", AdminStudentApi.normalizeStatus("  dropped-out  "));
        assertEquals("TRANSFERRED", AdminStudentApi.normalizeStatus("Transferred"));
        assertEquals("GRADUATED", AdminStudentApi.normalizeStatus("Graduated"));
        assertEquals("SUSPENDED", AdminStudentApi.normalizeStatus("Suspended"));
        assertNull(AdminStudentApi.normalizeStatus(null));
        assertNull(AdminStudentApi.normalizeStatus(""));
        assertNull(AdminStudentApi.normalizeStatus("   "));
    }

    @Test
    @DisplayName("AdminStudentApi.normalizeStatus rejects unsupported status values with IllegalArgumentException")
    void normalizeStatus_rejectsUnsupportedStatuses() {
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> AdminStudentApi.normalizeStatus("Archived"));
        assertTrue(ex1.getMessage().contains("Unsupported student status"));

        assertThrows(
                IllegalArgumentException.class,
                () -> AdminStudentApi.normalizeStatus("PENDING"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new AdminStudentApi.UpdateStudentRequest(
                        "Sarah Achieng", "FEMALE", LocalDate.of(2010, 5, 14), 1L, 2L, "InvalidStatus"));
    }

    @Test
    @DisplayName("UpdateStudentRequest compact constructor normalizes status automatically")
    void updateStudentRequest_normalizesStatusInCompactConstructor() {
        AdminStudentApi.UpdateStudentRequest req = new AdminStudentApi.UpdateStudentRequest(
                "Sarah Achieng", "FEMALE", LocalDate.of(2010, 5, 14), 1L, 2L, "Active");
        assertEquals("ACTIVE", req.status());
        assertTrue(AdminStudentApi.VALID_STATUSES.contains(req.status()));
    }

    @Test
    @DisplayName("buildUpdateRequest preserves existing gender, dateOfBirth, and streamId when class is unchanged")
    void buildUpdateRequest_preservesExistingFieldsWhenClassUnchanged() {
        LocalDate dob = LocalDate.of(2009, 8, 21);
        StudentsController.StudentRow row = new StudentsController.StudentRow(
                10L, "John Kato", "ADM010", "S3", "ACTIVE", 1L, "MALE", dob, 2L);

        AdminStudentApi.UpdateStudentRequest request = EditStudentController.buildUpdateRequest(
                row, "John Kato Updated", 1L, "Inactive");

        assertEquals("John Kato Updated", request.fullName());
        assertEquals("MALE", request.gender());
        assertEquals(dob, request.dateOfBirth());
        assertEquals(1L, request.currentClassId());
        assertEquals(2L, request.currentStreamId());
        assertEquals("INACTIVE", request.status());
    }

    @Test
    @DisplayName("buildUpdateRequest clears streamId when student is moved to a different class")
    void buildUpdateRequest_clearsStreamIdWhenClassChanged() {
        LocalDate dob = LocalDate.of(2009, 8, 21);
        StudentsController.StudentRow row = new StudentsController.StudentRow(
                10L, "John Kato", "ADM010", "S3", "ACTIVE", 1L, "MALE", dob, 2L);

        AdminStudentApi.UpdateStudentRequest request = EditStudentController.buildUpdateRequest(
                row, "John Kato", 2L, "Active");

        assertEquals(2L, request.currentClassId());
        assertNull(request.currentStreamId());
        assertEquals("MALE", request.gender());
        assertEquals(dob, request.dateOfBirth());
        assertEquals("ACTIVE", request.status());
    }

    @Test
    @DisplayName("edit-student.fxml configures admissionNumberField as read-only and disabled")
    void editStudentFxml_marksAdmissionNumberFieldReadOnly() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fxml/admin/edit-student.fxml")) {
            assertNotNull(in, "edit-student.fxml resource should exist");
            String fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(fxml.contains("fx:id=\"admissionNumberField\""));
            assertTrue(fxml.contains("editable=\"false\""));
            assertTrue(fxml.contains("disable=\"true\""));
        }
    }
}
