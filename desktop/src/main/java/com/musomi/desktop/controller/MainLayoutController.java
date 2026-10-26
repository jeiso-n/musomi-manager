package com.musomi.desktop.controller;

import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.controller.teacher.MarkEntryController;
import com.musomi.desktop.config.SceneManager;
import com.musomi.desktop.config.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainLayoutController {

    private static final Logger log =
            LoggerFactory.getLogger(MainLayoutController.class);

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox adminNavSection;

    @FXML
    private VBox teacherNavSection;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button markEntryButton;

    @FXML
    private Button myClassesButton;

    @FXML
    private Button assessmentsButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Label roleLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label sectionRoleLabel;

    @FXML
    private void initialize() {
        Session session = Session.getInstance();
        String role = normalizeRole(session.getRole());
        boolean isTeacher = "TEACHER".equals(role);
        boolean isAdmin = "ADMIN".equals(role);
        setSectionVisible(teacherNavSection, isTeacher);
        setSectionVisible(adminNavSection, isAdmin);
        log.info("Initialising main layout for role={}", role);
        if (roleLabel != null) {
            roleLabel.setText(role.isEmpty() ? "User" : capitalize(role));
        }
        if (sectionRoleLabel != null) {
            sectionRoleLabel.setText(role.isEmpty() ? "USER" : role);
        }
        if (userNameLabel != null) {
            String fullName = session.getUser() == null ? null : session.getUser().getFullName();
            userNameLabel.setText(fullName == null || fullName.isBlank() ? "User" : fullName);
        }

        if (isTeacher && teacherNavSection != null) {
            for (Node child : teacherNavSection.getChildren()) {
                if (child instanceof Button button && "Settings".equals(button.getText())) {
                    button.setVisible(false);
                    button.setManaged(false);
                }
            }
        }

        // Open Dashboard when the application starts.
        showDashboard();
    }

    private void setSectionVisible(VBox section, boolean visible) {
        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    private String capitalize(String value) {
        String normalized = value.toLowerCase();
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @FXML
    private void showDashboard() {

        setActiveButton(dashboardButton);

        loadView("/fxml/dashboard.fxml");
    }


    // =========================================================
    // MARK ENTRY
    // =========================================================

    @FXML
    private void showMarkEntry() {
        openMarkEntry(null);
    }

    private void openMarkEntry(Assessment assessment) {
        setActiveButton(markEntryButton);

        loadView("/fxml/teacher/mark-entry.fxml", assessment);
    }


    // =========================================================
    // MY CLASSES
    // =========================================================

    @FXML
    private void showMyClasses() {

        setActiveButton(myClassesButton);

        loadView("/fxml/teacher/my-classes.fxml");
    }


    // =========================================================
    // ASSESSMENTS
    // =========================================================

    @FXML
    private void showAssessments() {

        setActiveButton(assessmentsButton);

        loadView("/fxml/teacher/assessments.fxml");
    }

    @FXML
    private void onReportsClicked() {
        setActiveButton(reportsButton);
        loadView("/fxml/report/reports.fxml");
    }


    // =========================================================
    // ADMIN NAVIGATION
    // =========================================================

    @FXML
    private void showUsers() {
        loadView("/fxml/admin/users.fxml");
    }

    @FXML
    private void showClasses() {
        loadView("/fxml/admin/classes.fxml");
    }

    @FXML
    private void showSubjects() {
        loadView("/fxml/admin/subjects.fxml");
    }

    @FXML
    private void showTopics() {
        loadView("/fxml/admin/topics.fxml");
    }

    @FXML
    private void showStudents() {
        loadView("/fxml/admin/students.fxml");
    }

    @FXML
    private void showSettings() {
        loadView("/fxml/admin/settings.fxml");
    }

    @FXML
    private void showAuditLog() {
        loadView("/fxml/admin/audit-log.fxml");
    }

    @FXML
    private void showTeacherAssignments() {
        loadView("/fxml/admin/teacher-assignments.fxml");
    }


    // =========================================================
    // ACTIVE NAVIGATION BUTTON
    // =========================================================

    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #475569;" +
            "-fx-font-size: 14px;" +
            "-fx-alignment: CENTER_LEFT;" +
            "-fx-padding: 11px 14px;" +
            "-fx-cursor: hand;";

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #eef4ff;" +
            "-fx-background-radius: 8px;" +
            "-fx-text-fill: #2563eb;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-alignment: CENTER_LEFT;" +
            "-fx-padding: 11px 14px;" +
            "-fx-cursor: hand;";

    private void setActiveButton(Button activeButton) {

        // Reset all tracked nav buttons to inactive
        if (dashboardButton   != null) dashboardButton.setStyle(INACTIVE_STYLE);
        if (markEntryButton   != null) markEntryButton.setStyle(INACTIVE_STYLE);
        if (myClassesButton   != null) myClassesButton.setStyle(INACTIVE_STYLE);
        if (assessmentsButton != null) assessmentsButton.setStyle(INACTIVE_STYLE);
        if (reportsButton     != null) reportsButton.setStyle(INACTIVE_STYLE);

        // Apply active style to the selected button
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }


    // =========================================================
    // VIEW LOADING
    // =========================================================

    private void loadView(String fxmlPath) {
        loadView(fxmlPath, null);
    }

    private void loadView(String fxmlPath, Assessment assessment) {
        if (!Session.getInstance().isAuthenticated()) {
            SceneManager.getInstance().switchTo("/fxml/login.fxml");
            return;
        }

        try {

            log.debug("Loading view: {}", fxmlPath);

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(fxmlPath)
                    );

            if ("/fxml/dashboard.fxml".equals(fxmlPath)) {
                loader.setController(new DashboardController());
            }

            Node view = loader.load();

            if (loader.getController() instanceof DashboardController dashboardController) {
                dashboardController.setOnOpenMarkEntry(this::openMarkEntry);
                dashboardController.setOnOpenAdminView(this::loadView);
                dashboardController.configureDashboard(view);
            } else if (loader.getController() instanceof MarkEntryController markEntryController
                    && assessment != null) {
                markEntryController.setData(assessment);
            }

            contentArea.getChildren().setAll(view);

            log.debug("Successfully loaded: {}", fxmlPath);

        } catch (Exception e) {
            log.error("Failed to load view: {}", fxmlPath, e);
        }
    }
}
