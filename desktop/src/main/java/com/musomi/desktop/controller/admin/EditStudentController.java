package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.AdminStudentApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class EditStudentController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField admissionNumberField;

    @FXML
    private ComboBox<String> classComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    private StudentsController.StudentRow studentRow;
    private boolean saved;
    private final AdminClassApi adminClassApi = new AdminClassApi();
    private final AdminStudentApi adminStudentApi = new AdminStudentApi();
    private List<ClassResponse> classes = List.of();

    @FXML
    private void initialize() {

        TaskRunner.run(
                adminClassApi::getClasses,
                loadedClasses -> {
                    classes = loadedClasses;
                    classComboBox.getItems().setAll(
                            loadedClasses.stream().map(ClassResponse::name).toList());
                    if (studentRow != null) {
                        classComboBox.setValue(studentRow.getClassName());
                    }
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to load classes." : error.getMessage())
        );

        statusComboBox.getItems().addAll(
                "Active",
                "Inactive"
        );
    }

    public void setStudentRow(
            StudentsController.StudentRow studentRow
    ) {

        this.studentRow = studentRow;

        nameField.setText(
                studentRow.getName()
        );

        admissionNumberField.setText(
                studentRow.getAdmissionNumber()
        );

        classComboBox.setValue(
                studentRow.getClassName()
        );

        statusComboBox.setValue(
                studentRow.getStatus()
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

        if (studentRow == null) {
            return;
        }

        String name =
                nameField.getText().trim();

        String admissionNumber =
                admissionNumberField
                        .getText()
                        .trim();

        String className =
                classComboBox.getValue();

        String status =
                statusComboBox.getValue();

        if (name.isEmpty()
                || admissionNumber.isEmpty()
                || className == null
                || status == null) {

            return;
        }

        Long classId = classes.stream()
                .filter(item -> item.name().equals(className))
                .map(ClassResponse::id)
                .findFirst()
                .orElse(studentRow.getClassId());
        AdminStudentApi.UpdateStudentRequest request =
                new AdminStudentApi.UpdateStudentRequest(
                        name, null, null, classId, null, status);
        TaskRunner.run(
                () -> adminStudentApi.updateStudent(studentRow.getId(), request),
                ignored -> {
                    saved = true;
                    Toast.success("Student updated");
                    statusComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to update student." : error.getMessage()));
    }

    public boolean isSaved() { return saved; }
    public String getName() { return nameField.getText().trim(); }
    public String getClassName() { return classComboBox.getValue(); }
    public String getStatus() { return statusComboBox.getValue(); }
}