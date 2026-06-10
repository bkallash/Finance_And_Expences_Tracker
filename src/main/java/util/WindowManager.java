package util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class WindowManager {

    public static final double AUTH_WIDTH = 620.0;
    public static final double AUTH_HEIGHT = 470.0;
    public static final double SIGNUP_WIDTH = 620.0;
    public static final double SIGNUP_HEIGHT = 700.0;
    public static final double DASHBOARD_WIDTH = 1350.0;
    public static final double DASHBOARD_HEIGHT = 780.0;

    private static final double MIN_WIDTH = 800.0;
    private static final double MIN_HEIGHT = 500.0;

    public static void switchSceneWithSize(ActionEvent event, String fxmlPath, String title,
                                           double width, double height, double minWidth, double minHeight) {
        try {
            Parent root = FXMLLoader.load(WindowManager.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("Unable to load page: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void applyFixedSceneWithSize(Stage stage, Parent root, String title,
                                               double width, double height) {
        stage.setScene(new Scene(root, width, height));
        stage.setTitle(title);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.centerOnScreen();
        stage.show();
    }

    public static void switchToDashboard(ActionEvent event) {
        switchSceneWithSize(event, "/fxml/dashboard.fxml", "Dashboard",
                DASHBOARD_WIDTH, DASHBOARD_HEIGHT, MIN_WIDTH, MIN_HEIGHT);
    }

    public static void switchToLogin(ActionEvent event) {
        switchSceneWithSize(event, "/fxml/login.fxml", "Finance Tracker",
                AUTH_WIDTH, AUTH_HEIGHT, AUTH_WIDTH, AUTH_HEIGHT);
    }

    public static void switchToSignup(ActionEvent event) {
        switchSceneWithSize(event, "/fxml/signup.fxml", "Sign Up",
                SIGNUP_WIDTH, SIGNUP_HEIGHT, SIGNUP_WIDTH, SIGNUP_HEIGHT);
    }

    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
