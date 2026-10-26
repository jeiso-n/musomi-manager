package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import com.musomi.desktop.api.AdminUserApi;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class AddUserController {

    private final AdminUserApi api = new AdminUserApi();
    private boolean userCreated;


    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private void initialize() {

        roleComboBox.getItems().addAll(
                "ADMIN",
                "TEACHER",
                "STUDENT"
        );
    }

    @FXML
    private void handleCancel() {

        roleComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleCreateUser() {

        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (fullName.isEmpty()
                || username.isEmpty()
                || password.length() < 8
                || role == null) {

            showAlert(
                    "Missing Information",
                    password.length() < 8
                            ? "Password must be at least 8 characters."
                            : "Please fill in all fields."
            );

            return;
        }

        AdminUserApi.CreateUserRequest request = new AdminUserApi.CreateUserRequest(
                username, password, fullName, null, null, role);
        TaskRunner.run(
                () -> {
                    api.createUser(request);
                    return null;
                },
                ignored -> {
                    userCreated = true;
                    Toast.success("User created");
                    roleComboBox.getScene().getWindow().hide();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to create user." : error.getMessage()));
    }

    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public boolean isUserCreated() {
        return userCreated;
    }

}