package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.AuthService;
import service.SessionManager;
import util.VailEmailUtil;
import util.WindowManager;

import java.io.IOException;

public class LoginController {
    private static final String ERROR_STYLE_CLASS = "error-text";
    private static final String SUCCESS_STYLE_CLASS = "success-text";

    private final AuthService authService = new AuthService();

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    protected void onLoginButtonClick() {
        clearInlineMessage();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (!VailEmailUtil.isValidEmailFormat(email)) {
            showError("Incorrect information: invalid email format.");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            showError("Incorrect information: password is required.");
            return;
        }

        loginButton.setDisable(true);
        javafx.concurrent.Task<Boolean> loginTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.validateUser(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            if (loginTask.getValue()) {
                try {
                    SessionManager.setLoggedInUserEmail(email);

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                    Parent root = loader.load();

                    DashboardController dashboardController = loader.getController();
                    dashboardController.setUserEmail(email);

                    Stage stage = (Stage) emailField.getScene().getWindow();
                    WindowManager.applyFixedSceneWithSize(stage, root, "Dashboard",
                            WindowManager.DASHBOARD_WIDTH, WindowManager.DASHBOARD_HEIGHT);
                } catch (IOException e) {
                    showError("Unable to load dashboard page.");
                    e.printStackTrace();
                }
            } else {
                showError("Incorrect email or password.");
            }
        });

        loginTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            showError("Unable to login. Please try again.");
            loginTask.getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    @FXML
    protected void onSignupButtonClick(ActionEvent event) {
        try {
            WindowManager.switchToSignup(event);
        } catch (Exception ex) {
            showError("Unable to open register page.");
        }
    }

    private void showError(String message) {
        WindowManager.showErrorAlert("Error", message);
    }

    public void showSuccessMessage(String message) {
        errorLabel.getStyleClass().remove(ERROR_STYLE_CLASS);
        if (!errorLabel.getStyleClass().contains(SUCCESS_STYLE_CLASS)) {
            errorLabel.getStyleClass().add(SUCCESS_STYLE_CLASS);
        }
        errorLabel.setText(message);
    }

    private void clearInlineMessage() {
        errorLabel.setText("");
        errorLabel.getStyleClass().remove(SUCCESS_STYLE_CLASS);
        if (!errorLabel.getStyleClass().contains(ERROR_STYLE_CLASS)) {
            errorLabel.getStyleClass().add(ERROR_STYLE_CLASS);
        }
    }
}
