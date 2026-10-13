
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MainLayoutController {

    private static final Logger log =
            LoggerFactory.getLogger(MainLayoutController.class);

    private static final String ROLE_ADMIN = "ADMIN";
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

    @FXML
    private void initialize() {
        applyRoleNavigation();
        navigateToDefaultScreen();
        setupGlobalSearch();
    }

    // ROLE-BASED NAVIGATION

    private void applyRoleNavigation() {
        String role = Session.getInstance().getRole();

        log.debug("Applying navigation for role: {}", role);

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);
        boolean isTeacher = ROLE_TEACHER.equalsIgnoreCase(role);

        setNavSectionVisible(adminNavSection, isAdmin);
        setNavSectionVisible(teacherNavSection, isTeacher);
    }

    private void setNavSectionVisible(VBox section, boolean visible) {
        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    private void navigateToDefaultScreen() {
        String role = Session.getInstance().getRole();

        if (ROLE_TEACHER.equalsIgnoreCase(role)) {
            showMyClasses();
        } else {
            showDashboard();
        }
    }

    // GLOBAL SEARCH

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
        if (searchText == null) {
            searchPopup.hide();
            return;
        }

        String search = searchText.toLowerCase().trim();

        if (search.isEmpty()) {
            searchPopup.hide();
            return;
        }

        List<SearchResult> results = new ArrayList<>();

        addNavigationResult(results, search, "Dashboard",
                "Open dashboard", "/fxml/dashboard.fxml");
        addNavigationResult(results, search, "Users",
                "Manage system users", "/fxml/admin/users.fxml");
        addNavigationResult(results, search, "Classes",
                "Manage school classes", "/fxml/admin/classes.fxml");
        addNavigationResult(results, search, "Subjects",
                "Manage school subjects", "/fxml/admin/subjects.fxml");
        addNavigationResult(results, search, "Topics",
                "Manage subject topics", "/fxml/admin/topics.fxml");
        addNavigationResult(results, search, "Students",
                "Manage student records", "/fxml/admin/students.fxml");
        addNavigationResult(results, search, "Settings",
                "Manage school settings", "/fxml/admin/settings.fxml");
        addNavigationResult(results, search, "Audit Log",
                "View system activity", "/fxml/admin/audit-log.fxml");
        addNavigationResult(results, search, "My Classes",
                "View your classes", "/fxml/teacher/my-classes.fxml");

        // Temporary sample student records; API integration comes later.
        addStudentResult(results, search, "John Doe", "STU001", "Senior One");
        addStudentResult(results, search, "Jane Smith", "STU002", "Senior Two");
        addStudentResult(results, search, "Michael Brown", "STU003", "Senior Three");
        addStudentResult(results, search, "Sarah Wilson", "STU004", "Senior Four");

        showSearchResults(results);
    }

    private void addNavigationResult(
            List<SearchResult> results,
            String search,
            String title,
            String description,
            String fxmlPath
    ) {
        if (title.toLowerCase().contains(search)
                || description.toLowerCase().contains(search)) {
            results.add(new SearchResult(
                    "PAGE",
                    title,
                    description,
                    () -> loadView(fxmlPath)
            ));
        }
    }

    private void addStudentResult(
            List<SearchResult> results,
            String search,
            String name,
            String admissionNumber,
            String className
    ) {
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
        VBox resultContainer = new VBox(4);
        resultContainer.setPadding(new Insets(8));
        resultContainer.setPrefWidth(380);

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

        if (point != null) {
            searchPopup.show(
                    globalSearchField,
                    point.getX(),
                    point.getY() + 6
            );
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

    // NAVIGATION HANDLERS

    @FXML
    private void showDashboard() {
        loadView("/fxml/dashboard.fxml");
    }

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
    private void showMyClasses() {
        loadView("/fxml/teacher/my-classes.fxml");
    }

    // LOAD VIEW

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Failed to load view: {}", fxmlPath, e);
        }
    }

    // LOGOUT

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

    // SEARCH RESULT MODEL

    private static class SearchResult {
        private final String type;
        private final String title;
        private final String description;
        private final Runnable action;

        private SearchResult(
                String type,
                String title,
                String description,
                Runnable action
        ) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.action = action;
        }
    }
}
