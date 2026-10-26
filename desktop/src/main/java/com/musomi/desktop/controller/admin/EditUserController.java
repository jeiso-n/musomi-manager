package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class EditUserController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    private UsersController.UserRow user;
    private boolean saved;

    @FXML
    private void initialize() {

        roleComboBox.getItems().addAll(
                "Administrator",
                "Teacher"
        );

        statusComboBox.getItems().addAll(
                "Active",
                "Inactive"
        );
    }

    public void setUser(UsersController.UserRow user) {

        this.user = user;

        fullNameField.setText(user.getName());

        usernameField.setText(user.getUsername());

        roleComboBox.setValue(user.getRole());

        statusComboBox.setValue(user.getStatus());
    }

    @FXML
    private void handleCancel() {

        statusComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleSave() {

        if (user == null) {
            return;
        }

        if (fullNameField.getText().trim().isEmpty()) return;
        user.setName(fullNameField.getText().trim());
        saved = true;

        statusComboBox.getScene()
                .getWindow()
                .hide();
    }

    public boolean isSaved() { return saved; }
    public String getFullName() { return fullNameField.getText().trim(); }
}