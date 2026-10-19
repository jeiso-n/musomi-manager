
package com.musomi.desktop.controller;

import com.musomi.desktop.config.SceneManager;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.service.AuthService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainLayoutController {

    private static final Logger log =
            LoggerFactory.getLogger(MainLayoutController.class);

    private static final String ROLE_TEACHER = "TEACHER";

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox adminNavSection;

    @FXML
    private VBox teacherNavSection;

    @FXML
    private TextField globalSearchField;

    private final AuthService authService = new AuthService();
    private final Popup searchPopup = new Popup();

    private Button activeNavigationButton;

    @FXML
    private void initialize() {
        applyRoleNavigation();
        setupGlobalSearch();
        navigateToDefaultScreen();
    }

    @FXML
    private void showTeacherAssignments() {
        loadView("/fxml/admin/teacher-assignments.fxml");
    }




    // ---------------------------------------------------------
    // ROLE-BASED NAVIGATION
    // ---------------------------------------------------------

    private void applyRoleNavigation() {
        /*
         * UI-development mode:
         * Keep the Admin navigation visible until backend
         * authentication and role handling are connected.
         */
        setNavSectionVisible(adminNavSection, true);
        setNavSectionVisible(teacherNavSection, false);

        log.debug("Admin navigation enabled for UI development");
    }

    private void setNavSectionVisible(VBox section, boolean visible) {
        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    private void navigateToDefaultScreen() {
        String role = Session.getInstance().getRole();

        if (role != null && ROLE_TEACHER.equalsIgnoreCase(role)) {
            showMyClasses();
        } else {
            showDashboard();
        }
    }

    // ---------------------------------------------------------
    // GLOBAL SEARCH
    // ---------------------------------------------------------

    private void setupGlobalSearch() {
        if (globalSearchField == null) {
            log.warn("Global search field is missing from main-layout.fxml");
            return;
        }

        globalSearchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        handleGlobalSearch(newValue)
        );

        globalSearchField.focusedProperty().addListener(
                (observable, oldValue, focused) -> {
                    if (!focused) {
                        searchPopup.hide();
                    }
                }
        );

        searchPopup.setAutoHide(true);
        searchPopup.setAutoFix(true);
    }

    private void handleGlobalSearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            searchPopup.hide();
            return;
        }

        String search = searchText.toLowerCase().trim();
        List<SearchResult> results = new ArrayList<>();

        addNavigationResult(results, search, "Dashboard",
                "Open dashboard", this::showDashboard);

        addNavigationResult(results, search, "Users",
                "Manage system users", this::showUsers);

        addNavigationResult(results, search, "Classes",
                "Manage school classes", this::showClasses);

        addNavigationResult(results, search, "Subjects",
                "Manage school subjects", this::showSubjects);

        addNavigationResult(results, search, "Teacher Assignments",
                "Assign teachers to classes and subjects", this::showTeacherAssignments);


        addNavigationResult(results, search, "Topics",
                "Manage subject topics", this::showTopics);

        addNavigationResult(results, search, "Students",
                "Manage student records", this::showStudents);

        addNavigationResult(results, search, "Settings",
                "Manage school settings", this::showSettings);

        addNavigationResult(results, search, "Audit Log",
                "View system activity", this::showAuditLog);

        addNavigationResult(results, search, "My Classes",
                "View your classes", this::showMyClasses);

        // Temporary sample student records until API integration.
        addStudentResult(results, search,
                "John Doe", "STU001", "Senior One");

        addStudentResult(results, search,
                "Jane Smith", "STU002", "Senior Two");

        addStudentResult(results, search,
                "Michael Brown", "STU003", "Senior Three");

        addStudentResult(results, search,
                "Sarah Wilson", "STU004", "Senior Four");

        showSearchResults(results);
    }

    private void addNavigationResult(
            List<SearchResult> results,
            String search,
            String title,
            String description,
            Runnable action) {

        if (title.toLowerCase().contains(search)
                || description.toLowerCase().contains(search)) {

            results.add(new SearchResult(
                    "PAGE",
                    title,
                    description,
                    action
            ));
        }
    }

    private void addStudentResult(
            List<SearchResult> results,
            String search,
            String name,
            String admissionNumber,
            String className) {

        if (name.toLowerCase().contains(search)
                || admissionNumber.toLowerCase().contains(search)
                || className.toLowerCase().contains(search)) {

            results.add(new SearchResult(
                    "STUDENT",
                    name,
                    admissionNumber + " • " + className,
                    this::showStudents
            ));
        }
    }

    private void showSearchResults(List<SearchResult> results) {
        if (globalSearchField == null) {
            return;
        }

        VBox resultContainer = new VBox(4);
        resultContainer.setPadding(new Insets(8));
        resultContainer.setPrefWidth(380);
        resultContainer.getStyleClass().add("popup");

        if (results.isEmpty()) {
            Label noResults = new Label("No results found");
            noResults.getStyleClass().add("search-no-results");
            noResults.setPadding(new Insets(14));
            resultContainer.getChildren().add(noResults);
        } else {
            Label heading = new Label("Search results");
            heading.getStyleClass().add("search-results-heading");
            heading.setPadding(new Insets(6, 10, 8, 10));
            resultContainer.getChildren().add(heading);

            for (SearchResult result : results) {
                resultContainer.getChildren().add(
                        createSearchResultButton(result)
                );
            }
        }

        searchPopup.getContent().setAll(resultContainer);

        Point2D point = globalSearchField.localToScreen(
                0, globalSearchField.getHeight()
        );

        if (point != null && globalSearchField.isFocused()) {
            if (!searchPopup.isShowing()) {
                searchPopup.show(
                        globalSearchField,
                        point.getX(),
                        point.getY() + 6
                );
            } else {
                searchPopup.setX(point.getX());
                searchPopup.setY(point.getY() + 6);
            }
        }
    }

    private Button createSearchResultButton(SearchResult result) {
        Label typeLabel = new Label(result.type);
        typeLabel.getStyleClass().add("search-result-type");

        Label titleLabel = new Label(result.title);
        titleLabel.getStyleClass().add("search-result-title");

        Label descriptionLabel = new Label(result.description);
        descriptionLabel.getStyleClass().add("search-result-description");

        VBox textContainer = new VBox(
                2, typeLabel, titleLabel, descriptionLabel
        );
        textContainer.setAlignment(Pos.CENTER_LEFT);

        HBox content = new HBox(textContainer);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("search-result-button");

        button.setOnAction(event -> {
            searchPopup.hide();
            globalSearchField.clear();
            result.action.run();
        });

        return button;
    }

    // ---------------------------------------------------------
    // NAVIGATION HANDLERS
    // ---------------------------------------------------------

    @FXML
    private void showDashboard() {
        navigate("/fxml/dashboard.fxml", "Dashboard");
    }

    @FXML
    private void showUsers() {
        navigate("/fxml/admin/users.fxml", "Users");
    }

    @FXML
    private void showClasses() {
        navigate("/fxml/admin/classes.fxml", "Classes");
    }

    @FXML
    private void showSubjects() {
        navigate("/fxml/admin/subjects.fxml", "Subjects");
    }

    @FXML
    private void showTopics() {
        navigate("/fxml/admin/topics.fxml", "Topics");
    }

    @FXML
    private void showStudents() {
        navigate("/fxml/admin/students.fxml", "Students");
    }

    @FXML
    private void showSettings() {
        navigate("/fxml/admin/settings.fxml", "Settings");
    }

    @FXML
    private void showAuditLog() {
        navigate("/fxml/admin/audit-log.fxml", "Audit Log");
    }

    @FXML
    private void showMyClasses() {
        navigate("/fxml/teacher/my-classes.fxml", "My Classes");
    }

    private void navigate(String fxmlPath, String pageName) {
        loadView(fxmlPath);

        /*
         * Only update the selected navigation item if the page
         * loaded successfully.
         */
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            setActiveNavigation(pageName);
        }
    }

    // ---------------------------------------------------------
    // ACTIVE SIDEBAR HIGHLIGHT
    // ---------------------------------------------------------

    private void setActiveNavigation(String pageName) {
        if (contentArea == null || contentArea.getScene() == null) {
            return;
        }

        Scene scene = contentArea.getScene();
        Parent root = scene.getRoot();

        List<Button> navigationButtons = new ArrayList<>();
        collectNavigationButtons(root, navigationButtons);

        activeNavigationButton = null;

        for (Button button : navigationButtons) {
            button.getStyleClass().remove("nav-button-active");

            if (pageName.equalsIgnoreCase(button.getText())) {
                activeNavigationButton = button;
            }
        }

        if (activeNavigationButton != null
                && !activeNavigationButton.getStyleClass()
                .contains("nav-button-active")) {
            activeNavigationButton.getStyleClass().add("nav-button-active");
        }
    }

    private void collectNavigationButtons(
            Parent parent,
            List<Button> buttons) {

        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Button button
                    && button.getParent() != null
                    && isNavigationButton(button)) {
                buttons.add(button);
            }

            if (node instanceof Parent child) {
                collectNavigationButtons(child, buttons);
            }
        }
    }

    private boolean isNavigationButton(Button button) {
        if (button.getText() == null) {
            return false;
        }

        String text = button.getText();

        return List.of(
                "Dashboard", "Users", "Classes", "Subjects",
                "Topics", "Students", "Settings", "Audit Log",
                "My Classes"
        ).contains(text);
    }

    // ---------------------------------------------------------
    // LOAD VIEW
    // ---------------------------------------------------------

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                log.error("FXML resource not found: {}", fxmlPath);
                return;
            }

            Node view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);

            log.debug("Loaded view: {}", fxmlPath);
        } catch (Exception e) {
            log.error("Failed to load view: {}", fxmlPath, e);
        }
    }

    // ---------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------

    @FXML
    private void handleLogout() {
        try {
            authService.logout();
        } catch (Exception e) {
            log.error("Logout failed", e);
        } finally {
            searchPopup.hide();
            SceneManager.getInstance().switchTo("/fxml/login.fxml");
        }
    }

    // ---------------------------------------------------------
    // SEARCH RESULT MODEL
    // ---------------------------------------------------------

    private static class SearchResult {
        private final String type;
        private final String title;
        private final String description;
        private final Runnable action;

        private SearchResult(
                String type,
                String title,
                String description,
                Runnable action) {

            this.type = type;
            this.title = title;
            this.description = description;
            this.action = action;
        }
    }
}
