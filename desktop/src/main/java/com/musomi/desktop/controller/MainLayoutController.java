package com.musomi.desktop.controller;

import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.controller.teacher.MarkEntryController;
import com.musomi.desktop.config.SceneManager;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.model.GlobalSearchResult;
import com.musomi.desktop.model.dto.UserResponse;
import com.musomi.desktop.service.AuthService;
import com.musomi.desktop.service.GlobalSearchService;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.controller.admin.EditUserController;
import com.musomi.desktop.controller.admin.UsersController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainLayoutController {

    private static final Logger log =
            LoggerFactory.getLogger(MainLayoutController.class);

    // =========================================================
    // SERVICES
    // =========================================================

    private final AuthService authService = new AuthService();

    private final com.musomi.desktop.api.AdminUserApi adminUserApi =
            new com.musomi.desktop.api.AdminUserApi();

    private final GlobalSearchService globalSearchService =
            new GlobalSearchService();

    // =========================================================
    // FXML COMPONENTS
    // =========================================================

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
    private Label topbarInitialsLabel;

    @FXML
    private Label sidebarInitialsLabel;

    @FXML
    private Label sidebarUserNameLabel;

    @FXML
    private Label sidebarUserRoleLabel;

    @FXML
    private TextField globalSearchField;

    // =========================================================
    // CURRENT USER
    // =========================================================

    private String currentRole = "";

    // =========================================================
    // NAVIGATION STYLES
    // =========================================================

    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent;" +
                    "-fx-background-radius: 7px;" +
                    "-fx-text-fill: #E0E7FF;" +
                    "-fx-font-size: 13px;" +
                    "-fx-alignment: CENTER_LEFT;" +
                    "-fx-padding: 12px 14px;" +
                    "-fx-cursor: hand;";

    private static final String ACTIVE_STYLE =
            "-fx-background-color: #4F46E5;" +
                    "-fx-background-radius: 7px;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-alignment: CENTER_LEFT;" +
                    "-fx-padding: 12px 14px;" +
                    "-fx-cursor: hand;";

    // =========================================================
    // PROFILE AND ACCOUNT SETTINGS
    // =========================================================

    @FXML
    private void showMyProfile() {
        loadView("/fxml/profile.fxml");
    }

    @FXML
    private void showAccountSettings() {
        loadView("/fxml/account-settings.fxml");
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    @FXML
    private void initialize() {

        Session session = Session.getInstance();

        currentRole = normalizeRole(session.getRole());

        boolean isTeacher = "TEACHER".equals(currentRole);
        boolean isAdmin = "ADMIN".equals(currentRole);

        // Connect and verify global search field
        if (globalSearchField != null) {

            log.info("Global search field connected successfully.");

            globalSearchField.setDisable(false);
            globalSearchField.setEditable(true);

            globalSearchField.setOnAction(event -> {
                log.info("Global search Enter key detected.");
                handleGlobalSearch();
            });

        } else {
            log.error("Global search field is NULL. Check MainLayout.fxml.");
        }

        // Display the correct navigation section
        setSectionVisible(teacherNavSection, isTeacher);
        setSectionVisible(adminNavSection, isAdmin);

        log.info("Initialising main layout for role={}", currentRole);

        // Display user role
        String displayRole = currentRole.isEmpty()
                ? "User"
                : capitalize(currentRole);

        if (roleLabel != null) {
            roleLabel.setText(displayRole);
        }

        if (sectionRoleLabel != null) {
            sectionRoleLabel.setText(
                    currentRole.isEmpty() ? "USER" : currentRole
            );
        }

        // Retrieve logged-in user's name
        String fullName = session.getUser() == null
                ? null
                : session.getUser().getFullName();

        String displayName = fullName == null || fullName.isBlank()
                ? "User"
                : fullName;

        if (userNameLabel != null) {
            userNameLabel.setText(displayName);
        }

        if (sidebarUserNameLabel != null) {
            sidebarUserNameLabel.setText(displayName);
        }

        if (sidebarUserRoleLabel != null) {
            sidebarUserRoleLabel.setText(displayRole);
        }

        // Generate user initials
        String initials = getInitials(displayName);

        if (topbarInitialsLabel != null) {
            topbarInitialsLabel.setText(initials);
        }

        if (sidebarInitialsLabel != null) {
            sidebarInitialsLabel.setText(initials);
        }

        // Hide teacher Settings button for now
        if (isTeacher && teacherNavSection != null) {

            for (Node child : teacherNavSection.getChildren()) {

                if (child instanceof Button button
                        && "Settings".equals(button.getText())) {

                    button.setVisible(false);
                    button.setManaged(false);
                }
            }
        }

        // Open dashboard when application starts
        showDashboard();
    }

    // =========================================================
    // GLOBAL SEARCH
    // =========================================================

    private void handleGlobalSearch() {

        String query = globalSearchField.getText();

        if (query == null || query.isBlank()) {
            return;
        }

        if (!"ADMIN".equals(currentRole)) {

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Global search is currently available to administrators only."
            ).showAndWait();

            return;
        }

        log.info("Starting global search for: {}", query);

        globalSearchField.setDisable(true);

        TaskRunner.run(
                () -> globalSearchService.search(query),

                results -> {

                    globalSearchField.setDisable(false);

                    log.info(
                            "Global search completed. Results found: {}",
                            results == null ? 0 : results.size()
                    );

                    if (results == null || results.isEmpty()) {

                        new Alert(
                                Alert.AlertType.INFORMATION,
                                "No matching records found."
                        ).showAndWait();

                        return;
                    }

                    ChoiceDialog<GlobalSearchResult> dialog =
                            new ChoiceDialog<>(results.get(0), results);

                    dialog.setTitle("Global Search");
                    dialog.setHeaderText("Search results");
                    dialog.setContentText("Select a record to open:");

                    dialog.showAndWait().ifPresent(result -> {

                        switch (result.category()) {

                            case "Student" -> showStudents();

                            case "User" -> openUserRecord(result.id());

                            case "Class" -> showClasses();

                            case "Subject" -> showSubjects();

                            case "Topic" -> showTopics();

                            default -> log.warn(
                                    "Unknown search result category: {}",
                                    result.category()
                            );
                        }
                    });
                },

                error -> {

                    globalSearchField.setDisable(false);

                    log.error("Global search failed", error);

                    new Alert(
                            Alert.AlertType.ERROR,
                            "Search failed: " + error.getMessage()
                    ).showAndWait();
                }
        );
    }


    private void openUserRecord(Long userId) {

        if (userId == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "The selected user has no valid ID."
            ).showAndWait();
            return;
        }

        TaskRunner.run(
                () -> adminUserApi.getUsers(0, 100, ""),
                result -> {

                    var user = result.data().stream()
                            .filter(item -> userId.equals(item.id()))
                            .findFirst()
                            .orElse(null);

                    if (user == null) {
                        new Alert(
                                Alert.AlertType.INFORMATION,
                                "The selected user could not be found."
                        ).showAndWait();
                        return;
                    }

                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                        "/fxml/admin/edit-user.fxml"
                                )
                        );

                        Parent root = loader.load();

                        EditUserController controller =
                                loader.getController();

                        String role = switch (user.role()) {
                            case "ADMIN" -> "Administrator";
                            case "TEACHER" -> "Teacher";
                            default -> user.role();
                        };

                        UsersController.UserRow userRow =
                                new UsersController.UserRow(
                                        user.id(),
                                        user.fullName(),
                                        user.username(),
                                        role,
                                        user.isActive() ? "Active" : "Inactive"
                                );

                        controller.setUser(userRow);

                        Stage dialog = new Stage();
                        dialog.setTitle("Edit User");
                        dialog.initModality(Modality.APPLICATION_MODAL);

                        Scene scene = new Scene(root);

                        scene.getStylesheets().add(
                                getClass().getResource("/css/styles.css")
                                        .toExternalForm()
                        );

                        scene.getStylesheets().add(
                                getClass().getResource("/css/components.css")
                                        .toExternalForm()
                        );

                        dialog.setScene(scene);
                        dialog.setResizable(false);
                        dialog.showAndWait();

                        if (controller.isSaved()) {

                            TaskRunner.run(
                                    () -> adminUserApi.updateUser(
                                            user.id(),
                                            controller.getFullName(),
                                            null,
                                            null
                                    ),
                                    ignored -> new Alert(
                                            Alert.AlertType.INFORMATION,
                                            "User updated successfully."
                                    ).showAndWait(),
                                    error -> new Alert(
                                            Alert.AlertType.ERROR,
                                            "Failed to update user: "
                                                    + error.getMessage()
                                    ).showAndWait()
                            );
                        }

                    } catch (Exception e) {
                        log.error("Unable to open selected user", e);

                        new Alert(
                                Alert.AlertType.ERROR,
                                "Unable to open user: " + e.getMessage()
                        ).showAndWait();
                    }
                },
                error -> {
                    log.error("Unable to retrieve selected user", error);

                    new Alert(
                            Alert.AlertType.ERROR,
                            "Unable to retrieve user: " + error.getMessage()
                    ).showAndWait();
                }
        );
    }


    // =========================================================
    // USER INITIALS
    // =========================================================

    private String getInitials(String name) {

        if (name == null || name.isBlank()) {
            return "U";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                parts[0].substring(0, 1)
                        + parts[parts.length - 1].substring(0, 1)
        ).toUpperCase();
    }

    // =========================================================
    // ROLE HELPERS
    // =========================================================

    private void setSectionVisible(VBox section, boolean visible) {

        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    private String capitalize(String value) {

        if (value == null || value.isBlank()) {
            return "User";
        }

        String normalized = value.toLowerCase();

        return normalized.substring(0, 1).toUpperCase()
                + normalized.substring(1);
    }

    private String normalizeRole(String role) {

        if (role == null) {
            return "";
        }

        String normalized = role.trim().toUpperCase();

        return normalized.startsWith("ROLE_")
                ? normalized.substring(5)
                : normalized;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @FXML
    private void showDashboard() {

        if ("ADMIN".equals(currentRole)) {
            setActiveAdminButton("Dashboard");
        } else {
            setActiveButton(dashboardButton);
        }

        loadView("/fxml/dashboard.fxml");
    }

    // =========================================================
    // TEACHER NAVIGATION
    // =========================================================

    @FXML
    private void showMarkEntry() {
        openMarkEntry(null);
    }

    private void openMarkEntry(Assessment assessment) {

        setActiveButton(markEntryButton);

        loadView("/fxml/teacher/mark-entry.fxml", assessment);
    }

    @FXML
    private void showMyClasses() {

        setActiveButton(myClassesButton);

        loadView("/fxml/teacher/my-classes.fxml");
    }

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

        setActiveAdminButton("Users");

        loadView("/fxml/admin/users.fxml");
    }

    @FXML
    private void showClasses() {

        setActiveAdminButton("Classes");

        loadView("/fxml/admin/classes.fxml");
    }

    @FXML
    private void showSubjects() {

        setActiveAdminButton("Subjects");

        loadView("/fxml/admin/subjects.fxml");
    }

    @FXML
    private void showTopics() {

        setActiveAdminButton("Topics");

        loadView("/fxml/admin/topics.fxml");
    }

    @FXML
    private void showStudents() {

        setActiveAdminButton("Students");

        loadView("/fxml/admin/students.fxml");
    }

    @FXML
    private void showSettings() {

        setActiveAdminButton("Settings");

        loadView("/fxml/admin/settings.fxml");
    }

    @FXML
    private void showAuditLog() {

        setActiveAdminButton("Audit Log");

        loadView("/fxml/admin/audit-log.fxml");
    }

    @FXML
    private void showTeacherAssignments() {

        setActiveAdminButton("Teacher Assignments");

        loadView("/fxml/admin/teacher-assignments.fxml");
    }

    // =========================================================
    // TEACHER ACTIVE NAVIGATION
    // =========================================================

    private void setActiveButton(Button activeButton) {

        if (dashboardButton != null) {
            dashboardButton.setStyle(INACTIVE_STYLE);
        }

        if (markEntryButton != null) {
            markEntryButton.setStyle(INACTIVE_STYLE);
        }

        if (myClassesButton != null) {
            myClassesButton.setStyle(INACTIVE_STYLE);
        }

        if (assessmentsButton != null) {
            assessmentsButton.setStyle(INACTIVE_STYLE);
        }

        if (reportsButton != null) {
            reportsButton.setStyle(INACTIVE_STYLE);
        }

        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }

    // =========================================================
    // ADMIN ACTIVE NAVIGATION
    // =========================================================

    private void setActiveAdminButton(String activeText) {

        if (adminNavSection == null) {
            return;
        }

        for (Node child : adminNavSection.getChildren()) {

            if (child instanceof Button button) {

                button.setStyle(INACTIVE_STYLE);

                if (activeText.equals(button.getText())) {
                    button.setStyle(ACTIVE_STYLE);
                }
            }
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout() {

        log.info("Logout requested");

        try {

            authService.logout();

            log.info("Logout completed successfully");

        } catch (RuntimeException e) {

            log.warn(
                    "Backend logout request failed. Local session was cleared.",
                    e
            );

        } finally {

            SceneManager.getInstance().switchTo("/fxml/login.fxml");
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

            FXMLLoader loader = new FXMLLoader(
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

            } else if (loader.getController() instanceof ProfileController profileController) {

                UserResponse user = Session.getInstance().getUser();

                profileController.setUser(user);
            }

            contentArea.getChildren().setAll(view);

            log.debug("Successfully loaded: {}", fxmlPath);

        } catch (Exception e) {

            log.error("Failed to load view: {}", fxmlPath, e);
        }
    }
}
