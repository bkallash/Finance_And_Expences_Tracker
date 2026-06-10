package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.User;
import service.SessionManager;
import service.UserService;
import util.WindowManager;

public class DashboardController {
    private static final String DEFAULT_WELCOME_TEXT = "Welcome to Dashboard!";
    private static final String REPORTS_FXML = "/fxml/reports.fxml";
    private static final String CATEGORIES_FXML = "/fxml/categories.fxml";
    private static final String TRANSACTIONS_FXML = "/fxml/transactions.fxml";
    private static final String STATEMENT_FXML = "/fxml/financial_statement.fxml";

    @FXML
    private Label welcomeLabel;

    @FXML
    private StackPane contentArea;

    private String userEmail;
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadContent(REPORTS_FXML);
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        updateWelcomeLabel();
    }

    private void updateWelcomeLabel() {
        if (userEmail == null || userEmail.isBlank()) {
            welcomeLabel.setText(DEFAULT_WELCOME_TEXT);
            return;
        }

        javafx.concurrent.Task<model.User> task = userService.getUserByEmailTask(userEmail);
        task.setOnSucceeded(e -> {
            model.User user = task.getValue();
            String firstName = user == null || user.getFirstName() == null ? "" : user.getFirstName().trim();
            welcomeLabel.setText(firstName.isEmpty() ? DEFAULT_WELCOME_TEXT : "Welcome, " + firstName + "!");
        });
        task.setOnFailed(e -> welcomeLabel.setText(DEFAULT_WELCOME_TEXT));
        new Thread(task).start();
    }

    @FXML
    protected void onCategoriesButtonClick(ActionEvent event) {
        loadContent(CATEGORIES_FXML);
    }

    @FXML
    protected void onTransactionsButtonClick(ActionEvent event) {
        loadContent(TRANSACTIONS_FXML);
    }

    @FXML
    protected void onReportsButtonClick(ActionEvent event) {
        loadContent(REPORTS_FXML);
    }

    @FXML
    protected void onStatementButtonClick(ActionEvent event) {
        loadContent(STATEMENT_FXML);
    }

    @FXML
    protected void onLogoutButtonClick(ActionEvent event) {
        SessionManager.clearSession();
        WindowManager.switchToLogin(event);
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent loadedContent = loader.load();

            contentArea.getChildren().setAll(loadedContent);
        } catch (Exception e) {
            WindowManager.showErrorAlert("Navigation Error", "Unable to load page: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
