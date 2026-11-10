package com.musomi.desktop.controller.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StudentsFxmlContractTest {

    @Test
    void studentManagementScreenExposesPortalAccountProvisioningAction() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/fxml/admin/students.fxml")) {
            assertNotNull(input, "Student management FXML must be available");
            String fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(fxml.contains("text=\"Create Student Logins\""));
            assertTrue(fxml.contains("onAction=\"#provisionStudentAccounts\""));
            assertTrue(fxml.contains("fx:id=\"actionsColumn\""),
                    "Student row actions must have a visible table column.");
        }
    }
}
