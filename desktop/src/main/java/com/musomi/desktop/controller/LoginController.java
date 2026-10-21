
package com.musomi.desktop.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import com.musomi.desktop.api.ApiException;
import com.musomi.desktop.config.AppConfig;
import com.musomi.desktop.config.SceneManager;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.service.AuthService;
import com.musomi.desktop.util.TaskRunner;

/**
 * Controller for the login screen.
 * Handles authentication and opens the main layout after successful login.
 */
public class LoginController implements Initializable {

    private static final Logger log =
            LoggerFactory.getLogger(LoginController.class);

    private static final String MAIN_LAYOUT_FXML =
            "/fxml/main-layout.fxml";

    @FXML private VBox leftPanel;
    @FXML private VBox rightPanel;
    @FXML private Label schoolNameLabel;
    @FXML private Label versionLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator spinner;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        versionLabel.setText(
                "Musomi Manager " +
                        AppConfig.getInstance().getAppVersion()
        );

        errorLabel.managedProperty()
                .bind(errorLabel.visibleProperty());

        spinner.managedProperty()
                .bind(spinner.visibleProperty());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        log.debug("Login attempt — user: {}", username);

        clearError();
        setLoading(true);

        TaskRunner.run(
                () -> authService.login(username, password),

                response -> {
                    log.debug("Login successful — user: {}", username);

                    // Diagnostic: check the role saved in the session.
                    System.out.println(
                            "ROLE AFTER LOGIN = " +
                                    Session.getInstance().getRole()
                    );

                    // Open the main layout after successful login.
                    SceneManager.getInstance()
                            .switchTo(MAIN_LAYOUT_FXML);
                },

                error -> {
                    log.debug(
                            "Login failed — user: {} — reason: {}",
                            username,
                            error.getMessage()
                    );

                    setLoading(false);
                    showError(resolveErrorMessage(error));
                }
        );
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        spinner.setVisible(loading);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private String resolveErrorMessage(Exception error) {
        if (error instanceof ApiException apiEx) {
            return switch (apiEx.getCode()) {
                case "INVALID_CREDENTIALS" ->
                        "Invalid username or password.";

                case "ACCOUNT_LOCKED" ->
                        apiEx.getMessage();

                case "ACCOUNT_INACTIVE" ->
                        "Your account has been deactivated. Contact an administrator.";

                default ->
                        apiEx.getMessage();
            };
        }

        return "Unable to connect. Please check your network and try again.";
    }
}
