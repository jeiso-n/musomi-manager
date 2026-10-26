package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.AdminStudentApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class AddStudentController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField admissionNumberField;

    @FXML
    private ComboBox<String> classComboBox;

    private final AdminClassApi adminClassApi = new AdminClassApi();
    private final AdminStudentApi adminStudentApi = new AdminStudentApi();
    private List<ClassResponse> classes = List.of();
    private boolean studentCreated;

    @FXML
    private void initialize() {

        TaskRunner.run(
                adminClassApi::getClasses,
                loadedClasses -> {
                    classes = loadedClasses;
                    classComboBox.getItems().setAll(
                            loadedClasses.stream().map(ClassResponse::name).toList());
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to load classes." : error.getMessage())
        );
    }

    @FXML
    private void handleCancel() {

        classComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleCreateStudent() {

        String name =
                nameField.getText().trim();

        String admissionNumber =
                admissionNumberField
                        .getText()
                        .trim();

        String className =
                classComboBox.getValue();

        if (name.isEmpty()
                || admissionNumber.isEmpty()
                || className == null) {

            showAlert(
                    "Missing Information",
                    "Please fill in all fields."
            );

            return;
        }

        Long classId = classes.stream()
                .filter(item -> item.name().equals(className))
                .map(ClassResponse::id)
                .findFirst()
                .orElse(null);
        if (classId == null) {
            showAlert("Invalid Class", "The selected class is no longer available.");
            return;
        }

        AdminStudentApi.CreateStudentRequest request =
                new AdminStudentApi.CreateStudentRequest(
                        admissionNumber, name, null, null, classId, null, null);
        TaskRunner.run(
                () -> adminStudentApi.createStudent(request),
                ignored -> {
                    studentCreated = true;
                    Toast.success("Student created");
                    classComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to create student." : error.getMessage()));
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

    public boolean isStudentCreated() {
        return studentCreated;
    }
}