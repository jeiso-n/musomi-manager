package com.musomi.desktop.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.api.TeacherAssessmentApi;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.model.AssessmentResponse;
import com.musomi.desktop.model.dto.TeacherClassResponse;
import com.musomi.desktop.model.dto.UserResponse;
import com.musomi.desktop.service.TeacherClassService;
import com.musomi.desktop.util.TaskRunner;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private record ClassKey(Long id, String name) {}

    private record ClassStreamKey(Long classId, String className, Long streamId, String streamName) {}

    private record AdminSummary(int students, int classes, int subjects, int users) {}

    record TeacherClassStats(int totalStudents, int totalClasses) {}

    record TeacherAssessmentStats(int total, int draft, int published) {}

    private Consumer<Assessment> onOpenMarkEntry;
    private Consumer<String> onOpenAdminView;

    public void setOnOpenMarkEntry(Consumer<Assessment> onOpenMarkEntry) {
        this.onOpenMarkEntry = onOpenMarkEntry;
    }

    public void setOnOpenAdminView(Consumer<String> onOpenAdminView) {
        this.onOpenAdminView = onOpenAdminView;
    }

    public void configureDashboard(Node root) {
        UserResponse user = Session.getInstance().getUser();
        if (user == null || !(root instanceof Parent parent)) {
            return;
        }

        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            configureTeacherDashboard(parent);
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            configureAdminDashboard(parent);
        }
    }

    private void configureTeacherDashboard(Parent root) {
        replaceTeacherQuickActions(root);
        updateCard(root, "Students", "My Students", "…", "Loading your classes...");
        updateCard(root, "Classes", "My Classes", "…", "Loading your classes...");
        updateCard(root, "Subjects", "Total Assessments", "…", "Loading your assessments...");
        updateCard(root, "Users", "Published Assessments", "…", "Loading your assessments...");

        TaskRunner.run(
                () -> new TeacherClassService().getMyClasses(),
                classes -> showTeacherStats(root, classes),
                error -> {
                    log.error("Unable to load teacher dashboard classes", error);
                    updateCard(root, "My Students", "My Students", "—",
                            "Unable to load class data");
                    updateCard(root, "My Classes", "My Classes", "—",
                            "Unable to load class data");
                });

        TaskRunner.run(
                () -> new TeacherAssessmentApi().getMyAssessments(),
                response -> showTeacherAssessmentStats(root, response),
                error -> {
                    log.error("Unable to load teacher dashboard assessments", error);
                    updateCard(root, "Total Assessments", "Total Assessments", "—",
                            "Unable to load assessment data");
                    updateCard(root, "Published Assessments", "Published Assessments", "—",
                            "Unable to load assessment data");
                });
    }

    private void configureAdminDashboard(Parent root) {
        replaceAdminQuickActions(root);
        updateCard(root, "Students", "Students", "…", "Loading from the server...");
        updateCard(root, "Classes", "Classes", "…", "Loading from the server...");
        updateCard(root, "Subjects", "Subjects", "…", "Loading from the server...");
        updateCard(root, "Users", "Users", "…", "Loading from the server...");

        TaskRunner.run(
                this::loadAdminSummary,
                summary -> {
                    updateCard(root, "Students", "Students",
                            String.valueOf(summary.students()), "Total students");
                    updateCard(root, "Classes", "Classes",
                            String.valueOf(summary.classes()), "Active classes");
                    updateCard(root, "Subjects", "Subjects",
                            String.valueOf(summary.subjects()), "Available subjects");
                    updateCard(root, "Users", "Users",
                            String.valueOf(summary.users()), "System users");
                },
                error -> {
                    log.error("Unable to load Admin dashboard summary", error);
                    for (String title : List.of("Students", "Classes", "Subjects", "Users")) {
                        updateCard(root, title, title, "—", "Unable to load data");
                    }
                });
    }

    private AdminSummary loadAdminSummary() {
        ApiClient apiClient = ApiClient.getInstance();
        JsonNode[] students = apiClient.get("/admin/students", JsonNode[].class);
        JsonNode[] classes = apiClient.get("/admin/classes", JsonNode[].class);
        JsonNode[] subjects = apiClient.get("/admin/subjects", JsonNode[].class);
        JsonNode usersPage = apiClient.get("/admin/users?size=1", JsonNode.class);
        if (students == null || classes == null || subjects == null
                || usersPage == null || !usersPage.has("total")) {
            throw new IllegalStateException("Admin dashboard API returned incomplete summary data");
        }

        return new AdminSummary(
                students.length,
                countActive(classes),
                countActive(subjects),
                usersPage.path("total").asInt());
    }

    private int countActive(JsonNode[] rows) {
        int count = 0;
        for (JsonNode row : rows) {
            if (row != null && row.path("isActive").asBoolean(false)) {
                count++;
            }
        }
        return count;
    }

    private void showTeacherStats(Parent root, TeacherClassResponse[] response) {
        showTeacherStats(root, response == null ? List.of() : Arrays.asList(response));
    }

    private void showTeacherStats(Parent root, List<TeacherClassResponse> response) {
        TeacherClassStats stats = calculateTeacherClassStats(response);

        updateCard(root, "My Students", "My Students", String.valueOf(stats.totalStudents()),
                "Students across your classes");
        updateCard(root, "My Classes", "My Classes", String.valueOf(stats.totalClasses()),
                "Classes you teach");
    }

    static TeacherClassStats calculateTeacherClassStats(List<TeacherClassResponse> response) {
        Set<ClassKey> classes = new HashSet<>();
        Set<ClassKey> wholeClasses = new HashSet<>();
        Set<ClassStreamKey> classStreams = new HashSet<>();
        int studentCount = 0;

        if (response != null) {
            for (TeacherClassResponse teacherClass : response) {
                if (teacherClass == null) {
                    continue;
                }
                Long classId = teacherClass.getClassId();
                String className = teacherClass.getClassName();
                Long streamId = teacherClass.getStreamId();
                String streamName = teacherClass.getStreamName();
                ClassKey classKey = new ClassKey(classId, classId == null ? className : null);
                classes.add(classKey);
                if (streamId == null && (streamName == null || streamName.isBlank())) {
                    wholeClasses.add(classKey);
                }
            }

            for (TeacherClassResponse teacherClass : response) {
                if (teacherClass == null) {
                    continue;
                }
                Long classId = teacherClass.getClassId();
                String className = teacherClass.getClassName();
                Long streamId = teacherClass.getStreamId();
                String streamName = teacherClass.getStreamName();
                ClassKey classKey = new ClassKey(classId, classId == null ? className : null);
                boolean isStreamSpecific = streamId != null
                        || (streamName != null && !streamName.isBlank());
                if (isStreamSpecific && wholeClasses.contains(classKey)) {
                    continue;
                }

                ClassStreamKey classStream = new ClassStreamKey(
                        classId, classId == null ? className : null,
                        streamId, streamId == null ? streamName : null);
                if (classStreams.add(classStream)) {
                    studentCount += teacherClass.getStudentCount();
                }
            }
        }

        return new TeacherClassStats(studentCount, classes.size());
    }

    private void showTeacherAssessmentStats(
            Parent root, List<AssessmentResponse> assessments) {
        TeacherAssessmentStats stats = calculateTeacherAssessmentStats(assessments);
        String totalDesc = stats.draft() > 0
                ? stats.draft() + " draft, " + stats.published() + " published"
                : "All your assessments";
        String publishedDesc = stats.draft() > 0
                ? stats.draft() + " draft pending"
                : "Published assessments";
        updateCard(root, "Total Assessments", "Total Assessments",
                String.valueOf(stats.total()), totalDesc);
        updateCard(root, "Pending Assessments", "Pending Assessments",
                String.valueOf(stats.draft()), "Draft assessments");
        updateCard(root, "Published Assessments", "Published Assessments",
                String.valueOf(stats.published()), publishedDesc);
    }

    static TeacherAssessmentStats calculateTeacherAssessmentStats(
            List<AssessmentResponse> assessments) {
        List<AssessmentResponse> data = assessments == null ? List.of() : assessments;
        int total = 0;
        int draft = 0;
        int published = 0;
        for (AssessmentResponse item : data) {
            if (item == null) {
                continue;
            }
            total++;
            if ("DRAFT".equalsIgnoreCase(item.status())) {
                draft++;
            } else if ("PUBLISHED".equalsIgnoreCase(item.status())) {
                published++;
            }
        }
        return new TeacherAssessmentStats(total, draft, published);
    }

    private void replaceTeacherQuickActions(Parent root) {
        Label heading = findLabel(root, "Quick Actions");
        if (heading == null || !(heading.getParent() instanceof VBox panel)) {
            return;
        }

        Button markEntry = new Button("Go to Mark Entry");
        markEntry.setMaxWidth(Double.MAX_VALUE);
        markEntry.setOnAction(event -> {
            if (onOpenMarkEntry != null) {
                onOpenMarkEntry.accept(null);
            }
        });
        panel.getChildren().setAll(heading, markEntry);
    }

    private void replaceAdminQuickActions(Parent root) {
        Label heading = findLabel(root, "Quick Actions");
        if (heading == null || !(heading.getParent() instanceof VBox panel)) {
            return;
        }

        Button students = createAdminAction("Manage Students", "/fxml/admin/students.fxml");
        Button classes = createAdminAction("Manage Classes", "/fxml/admin/classes.fxml");
        Button subjects = createAdminAction("Manage Subjects", "/fxml/admin/subjects.fxml");
        panel.getChildren().setAll(heading, students, classes, subjects);
    }

    private Button createAdminAction(String text, String viewPath) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            if (onOpenAdminView != null) {
                onOpenAdminView.accept(viewPath);
            }
        });
        return button;
    }

    private void updateCard(Parent root, String currentTitle, String title,
                            String value, String description) {
        Label titleLabel = findLabel(root, currentTitle);
        if (titleLabel == null || !(titleLabel.getParent() instanceof VBox card)) {
            return;
        }

        titleLabel.setText(title);
        List<Label> labels = card.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .toList();
        if (labels.size() >= 3) {
            labels.get(1).setText(value);
            labels.get(2).setText(description);
        }
    }

    private void hideCard(Parent root, String title) {
        Label titleLabel = findLabel(root, title);
        if (titleLabel != null && titleLabel.getParent() instanceof VBox card) {
            card.setVisible(false);
            card.setManaged(false);
        }
    }

    private Label findLabel(Parent parent, String text) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Label label && text.equals(label.getText())) {
                return label;
            }
            if (child instanceof Parent nested) {
                Label found = findLabel(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
