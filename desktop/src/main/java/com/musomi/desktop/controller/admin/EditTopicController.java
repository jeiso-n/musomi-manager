package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

import com.musomi.desktop.api.AdminTopicApi;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class EditTopicController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> subjectComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    private TopicsController.TopicRow topicRow;
    private boolean saved;
    private final AdminSubjectApi adminSubjectApi = new AdminSubjectApi();
    private final AdminTopicApi adminTopicApi = new AdminTopicApi();
    private List<SubjectResponse> subjects = List.of();

    @FXML
    private void initialize() {

        TaskRunner.run(
                adminSubjectApi::getSubjects,
                loadedSubjects -> {
                    subjects = loadedSubjects;
                    subjectComboBox.getItems().setAll(
                            loadedSubjects.stream().map(SubjectResponse::name).toList());
                    if (topicRow != null) {
                        subjectComboBox.setValue(topicRow.getSubject());
                    }
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to load subjects." : error.getMessage())
        );

        statusComboBox.getItems().addAll(
                "Active",
                "Inactive"
        );
    }

    public void setTopicRow(
            TopicsController.TopicRow topicRow
    ) {

        this.topicRow = topicRow;

        nameField.setText(topicRow.getName());

        subjectComboBox.setValue(
                topicRow.getSubject()
        );

        statusComboBox.setValue(
                topicRow.getStatus()
        );
    }

    @FXML
    private void handleCancel() {

        statusComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleSave() {

        if (topicRow == null) {
            return;
        }

        String name =
                nameField.getText().trim();

        String subject =
                subjectComboBox.getValue();

        String status =
                statusComboBox.getValue();

        if (name.isEmpty()
                || subject == null
                || status == null) {

            return;
        }

        Long subjectId = subjects.stream()
                .filter(item -> item.name().equals(subject))
                .map(SubjectResponse::id)
                .findFirst()
                .orElse(topicRow.getSubjectId());
        AdminTopicApi.UpdateTopicRequest request =
                new AdminTopicApi.UpdateTopicRequest(subjectId, name, null, 0);
        TaskRunner.run(
                () -> adminTopicApi.updateTopic(topicRow.getId(), request),
                ignored -> {
                    saved = true;
                    Toast.success("Topic updated");
                    statusComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to update topic." : error.getMessage()));
    }

    public String getName() {
        return nameField.getText().trim();
    }

    public String getSubject() {
        return subjectComboBox.getValue();
    }

    public boolean isSaved() {
        return saved;
    }
}