package com.musomi.desktop.controller;

import java.util.List;
import java.util.function.Consumer;

import com.musomi.desktop.config.Session;
import com.musomi.desktop.controller.teacher.AssessmentStore;
import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.model.dto.UserResponse;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DashboardController {

    // Dashboard sections
    @FXML private VBox adminDashboard;
    @FXML private VBox teacherDashboard;

    // Teacher dashboard fields
    @FXML private Label myClassesCount;
    @FXML private Label totalStudentsCount;
    @FXML private Label pendingMarksCount;
    @FXML private Label publishedAssessmentsCount;
    @FXML private Label pendingReminderText;
    @FXML private Label greetingLabel;
    @FXML private Button markEntryButton;

    private Consumer<Assessment> onOpenMarkEntry;

    @FXML
    private void initialize() {
        UserResponse user = Session.getInstance().getUser();

        String role = user == null || user.getRole() == null
                ? ""
                : user.getRole().trim();

        boolean isTeacher = "TEACHER".equalsIgnoreCase(role);

        // Show only the dashboard matching the user's role.
        if (adminDashboard != null) {
            adminDashboard.setVisible(!isTeacher);
            adminDashboard.setManaged(!isTeacher);
        }

        if (teacherDashboard != null) {
            teacherDashboard.setVisible(isTeacher);
            teacherDashboard.setManaged(isTeacher);
        }

        if (isTeacher) {
            refreshPendingReminder();
            refreshSummaryCards(user);
        }
    }

    // Admin dashboard actions

    @FXML
    private void handleAddStudent() {
        openDialog("/fxml/admin/add-student.fxml", "Add Student");
    }

    @FXML
    private void handleAddClass() {
        openDialog("/fxml/admin/add-class.fxml", "Add Class");
    }

    @FXML
    private void handleAddSubject() {
        openDialog("/fxml/admin/add-subject.fxml", "Add Subject");
    }

    private void openDialog(String fxmlPath, String title) {
        try {
            var resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                System.err.println("FXML file not found: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(resource);
            Stage dialog = new Stage();

            dialog.setTitle(title);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(createStyledScene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Scene createStyledScene(Parent root) {
        Scene scene = new Scene(root);

        var styles = getClass().getResource("/css/styles.css");
        var components = getClass().getResource("/css/components.css");

        if (styles != null) {
            scene.getStylesheets().add(styles.toExternalForm());
        }

        if (components != null) {
            scene.getStylesheets().add(components.toExternalForm());
        }

        return scene;
    }

    // Teacher assessment actions

    public void setOnOpenMarkEntry(
            Consumer<Assessment> onOpenMarkEntry) {
        this.onOpenMarkEntry = onOpenMarkEntry;
    }

    @FXML
    private void goToMarkEntry() {
        List<Assessment> pending =
                AssessmentStore.getPendingAssessments();

        if (!pending.isEmpty() && onOpenMarkEntry != null) {
            onOpenMarkEntry.accept(pending.get(0));
        }
    }

    private void refreshPendingReminder() {
        int count = AssessmentStore.getPendingAssessments().size();

        if (pendingMarksCount != null) {
            pendingMarksCount.setText(String.valueOf(count));
        }

        if (pendingReminderText != null) {
            pendingReminderText.setText(count == 0
                    ? "All assessments are fully marked and published."
                    : count + (count == 1
                    ? " assessment is incomplete or not yet published."
                    : " assessments are incomplete or not yet published."));
        }

        if (markEntryButton != null) {
            boolean hasPending = count > 0;
            markEntryButton.setVisible(hasPending);
            markEntryButton.setManaged(hasPending);
        }
    }

    private void refreshSummaryCards(UserResponse user) {
        if (greetingLabel != null) {
            if (user == null || user.getFullName() == null
                    || user.getFullName().isBlank()) {
                greetingLabel.setText("Welcome back");
            } else {
                String firstName =
                        user.getFullName().trim().split("\\s+")[0];

                greetingLabel.setText("Good morning, " + firstName);
            }
        }

        if (myClassesCount != null) {
            myClassesCount.setText(
                    String.valueOf(AssessmentStore.countDistinctClasses()));
        }

        if (totalStudentsCount != null) {
            totalStudentsCount.setText(
                    String.valueOf(AssessmentStore.mockRosterSize()));
        }

        if (publishedAssessmentsCount != null) {
            publishedAssessmentsCount.setText(
                    String.valueOf(AssessmentStore.countPublished()));
        }
    }
}
