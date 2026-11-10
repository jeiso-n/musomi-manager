package com.musomi.desktop.controller;

import com.musomi.desktop.model.dto.UserResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label initialsLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label phoneLabel;

    @FXML
    private Label schoolLabel;

    public void setUser(UserResponse user) {

        if (user == null) {
            fullNameLabel.setText("Unavailable");
            initialsLabel.setText("--");
            roleLabel.setText("Unavailable");
            usernameLabel.setText("Unavailable");
            emailLabel.setText("Unavailable");
            phoneLabel.setText("Unavailable");
            schoolLabel.setText("Unavailable");
            return;
        }

        String fullName = valueOrFallback(user.fullName());

        fullNameLabel.setText(fullName);
        initialsLabel.setText(getInitials(fullName));
        roleLabel.setText(formatRole(user.role()));

        usernameLabel.setText(valueOrFallback(user.username()));
        emailLabel.setText(valueOrFallback(user.email()));
        phoneLabel.setText(valueOrFallback(user.phone()));
        schoolLabel.setText(valueOrFallback(user.schoolName()));
    }

    private String valueOrFallback(String value) {
        return value == null || value.isBlank()
                ? "Not provided"
                : value;
    }

    private String getInitials(String fullName) {

        if (fullName == null || fullName.isBlank()) {
            return "--";
        }

        String[] names = fullName.trim().split("\\s+");

        if (names.length == 1) {
            return names[0].substring(0, 1).toUpperCase();
        }

        return (names[0].substring(0, 1)
                + names[names.length - 1].substring(0, 1))
                .toUpperCase();
    }

    private String formatRole(String role) {

        if (role == null || role.isBlank()) {
            return "Not provided";
        }

        return role.replace("_", " ").toLowerCase()
                .substring(0, 1).toUpperCase()
                + role.replace("_", " ").toLowerCase().substring(1);
    }
}
