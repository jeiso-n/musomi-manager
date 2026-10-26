package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.api.AdminTopicApi;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class AddTopicController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> subjectComboBox;

    private final AdminSubjectApi adminSubjectApi = new AdminSubjectApi();
    private final AdminTopicApi adminTopicApi = new AdminTopicApi();
    private List<SubjectResponse> subjects = List.of();
    private boolean topicCreated;

    @FXML
    private void initialize() {

        TaskRunner.run(
                adminSubjectApi::getSubjects,
                loadedSubjects -> {
                    subjects = loadedSubjects;
                    subjectComboBox.getItems().setAll(
                            loadedSubjects.stream().map(SubjectResponse::name).toList());
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to load subjects." : error.getMessage())
        );
    }

    @FXML
    private void handleCancel() {

        subjectComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleCreateTopic() {

        String name =
                nameField.getText().trim();

        String subject =
                subjectComboBox.getValue();

        if (name.isEmpty() || subject == null) {

            showAlert(
                    "Missing Information",
                    "Please fill in all fields."
            );

            return;
        }

        Long subjectId = subjects.stream()
                .filter(item -> item.name().equals(subject))
                .map(SubjectResponse::id)
                .findFirst()
                .orElse(null);
        if (subjectId == null) {
            showAlert("Invalid Subject", "The selected subject is no longer available.");
            return;
        }

        AdminTopicApi.CreateTopicRequest request =
                new AdminTopicApi.CreateTopicRequest(subjectId, name, null, 0);
        TaskRunner.run(
                () -> adminTopicApi.createTopic(request),
                ignored -> {
                    topicCreated = true;
                    Toast.success("Topic created");
                    subjectComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to create topic." : error.getMessage()));
    }

    private void showAlert(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }

    public boolean isTopicCreated() {
        return topicCreated;
    }
}