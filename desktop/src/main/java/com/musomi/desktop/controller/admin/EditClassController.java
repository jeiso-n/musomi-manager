package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class EditClassController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> levelComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    private ClassesController.ClassRow classRow;
    private boolean saved;
    private final AdminClassApi adminClassApi = new AdminClassApi();

    @FXML
    private void initialize() {

        levelComboBox.getItems().addAll(
                "O-Level",
                "A-Level"
        );

        statusComboBox.getItems().addAll(
                "Active",
                "Inactive"
        );
    }

    public void setClassRow(
            ClassesController.ClassRow classRow
    ) {

        this.classRow = classRow;

        nameField.setText(
                classRow.getName()
        );

        levelComboBox.setValue(
                classRow.getLevel()
        );

        statusComboBox.setValue(
                classRow.getStatus()
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

        if (classRow == null) {
            return;
        }

        String name =
                nameField.getText().trim();

        String level =
                levelComboBox.getValue();

        String status =
                statusComboBox.getValue();

        if (name.isEmpty()
                || level == null
                || status == null) {
            return;
        }

        AdminClassApi.UpdateClassRequest request = new AdminClassApi.UpdateClassRequest(
                name, classRow.getClassLevelId(), classRow.getAcademicYearId(), null);
        TaskRunner.run(
                () -> adminClassApi.updateClass(classRow.getId(), request),
                ignored -> {
                    saved = true;
                    Toast.success("Class updated");
                    statusComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to update class." : error.getMessage()));
    }

    public boolean isSaved() { return saved; }
    public String getName() { return nameField.getText().trim(); }
    public String getLevel() { return levelComboBox.getValue(); }
}