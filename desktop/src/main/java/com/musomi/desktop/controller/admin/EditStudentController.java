package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

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
        configureReadOnlyAdmissionNumber(admissionNumberField);

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

        statusComboBox.getItems().setAll(AdminStudentApi.VALID_STATUSES);
    }

    static void configureReadOnlyAdmissionNumber(TextField field) {
        if (field != null) {
            field.setEditable(false);
            field.setDisable(true);
            field.setFocusTraversable(false);
        }
    }

    public void setStudentRow(
            StudentsController.StudentRow studentRow
    ) {

        this.studentRow = studentRow;
        configureReadOnlyAdmissionNumber(admissionNumberField);

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
                AdminStudentApi.normalizeStatus(studentRow.getStatus())
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

        String className =
                classComboBox.getValue();

        String status =
                statusComboBox.getValue();

        if (name.isEmpty()
                || className == null
                || status == null) {

            return;
        }

        Long classId = classes.stream()
                .filter(item -> item.name().equals(className))
                .map(ClassResponse::id)
                .findFirst()
                .orElse(studentRow.getClassId());
        final AdminStudentApi.UpdateStudentRequest request;
        try {
            request = buildUpdateRequest(studentRow, name, classId, status);
        } catch (IllegalArgumentException ex) {
            Toast.error(ex.getMessage());
            return;
        }
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

    static AdminStudentApi.UpdateStudentRequest buildUpdateRequest(
            StudentsController.StudentRow studentRow,
            String fullName,
            Long selectedClassId,
            String status) {
        Long effectiveClassId = selectedClassId != null
                ? selectedClassId
                : (studentRow != null ? studentRow.getClassId() : null);
        Long preservedStreamId = studentRow != null
                && Objects.equals(effectiveClassId, studentRow.getClassId())
                ? studentRow.getCurrentStreamId()
                : null;
        return new AdminStudentApi.UpdateStudentRequest(
                fullName,
                studentRow != null ? studentRow.getGender() : null,
                studentRow != null ? studentRow.getDateOfBirth() : null,
                effectiveClassId,
                preservedStreamId,
                AdminStudentApi.normalizeStatus(status)
        );
    }

    public boolean isSaved() { return saved; }
    public String getName() { return nameField.getText().trim(); }
    public String getAdmissionNumber() {
        return studentRow != null
                ? studentRow.getAdmissionNumber()
                : (admissionNumberField != null ? admissionNumberField.getText().trim() : null);
    }
    public String getClassName() { return classComboBox.getValue(); }
    public String getStatus() { return statusComboBox.getValue(); }
}