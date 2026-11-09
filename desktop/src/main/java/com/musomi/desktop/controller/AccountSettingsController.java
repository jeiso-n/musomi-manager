package com.musomi.desktop.controller;

import com.musomi.desktop.service.AuthService;
import com.musomi.desktop.util.TaskRunner;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class AccountSettingsController {

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button changePasswordButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        feedbackLabel.setText("");
    }

    @FXML
    private void handleChangePassword() {

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {

            showFeedback("Please complete all password fields.", false);
            return;
        }

        if (newPassword.length() < 8) {
            showFeedback(
                    "Your new password must contain at least 8 characters.",
                    false
            );
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showFeedback(
                    "The new password and confirmation do not match.",
                    false
            );
            return;
        }

        if (currentPassword.equals(newPassword)) {
            showFeedback(
                    "Your new password must be different from your current password.",
                    false
            );
            return;
        }

        changePasswordButton.setDisable(true);
        changePasswordButton.setText("Changing...");
        showFeedback("Updating your password...", false);

        TaskRunner.run(
                () -> {
                    authService.changePassword(currentPassword, newPassword);
                    return null;
                },
                result -> {
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();

                    changePasswordButton.setDisable(false);
                    changePasswordButton.setText("Change Password");

                    showFeedback(
                            "Your password has been changed successfully.",
                            true
                    );
                },
                error -> {
                    changePasswordButton.setDisable(false);
                    changePasswordButton.setText("Change Password");

                    String message = error.getMessage();

                    if (message == null || message.isBlank()) {
                        message = "Unable to change your password. Please try again.";
                    }

                    showFeedback(message, false);
                }
        );
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);

        feedbackLabel.setStyle(
                success
                        ? "-fx-text-fill: #15803D;"
                        : "-fx-text-fill: #DC2626;"
        );
    }
}
