package controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import model.Category;
import model.Transaction;
import model.User;
import service.CategoryService;
import service.SessionManager;
import service.TransactionService;
import service.UserService;
import util.WindowManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class FinancialStatementController {

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private Button generateButton;

    @FXML
    private Button cancelButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea statementTextArea;

    private final TransactionService transactionService = new TransactionService();
    private final CategoryService categoryService = new CategoryService();
    private final UserService userService = new UserService();
    private Task<String> currentTask;

    @FXML
    public void initialize() {
        loadCategories();
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());
    }

    private void loadCategories() {
        Task<List<Category>> task = categoryService.getAllCategoriesTask();
        task.setOnSucceeded(e -> {
            categoryComboBox.setItems(FXCollections.observableArrayList(task.getValue()));
            configureCategoryComboBoxDisplay();
        });
        new Thread(task).start();
    }

    @FXML
    protected void onGenerateButtonClick() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        Category category = categoryComboBox.getValue();

        if (from == null || to == null) {
            WindowManager.showErrorAlert("Validation Error", "Please select both start and end dates.");
            return;
        }

        if (from.isAfter(to)) {
            WindowManager.showErrorAlert("Validation Error", "Start date cannot be after end date.");
            return;
        }

        User user = userService.getUserByEmail(SessionManager.getLoggedInUserEmail());
        if (user == null) {
            WindowManager.showErrorAlert("Error", "No logged-in user found.");
            return;
        }

        statementTextArea.clear();
        
        currentTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Retrieving transactions from database...");
                List<Transaction> transactions = transactionService.getTransactionsByCriteria(user, from, to, category);
                
                if (transactions.isEmpty()) {
                    updateMessage("No results found.");
                    return "No transactions found for the selected range.";
                }

                int totalCount = transactions.size();
                double income = 0;
                double expenses = 0;
                StringBuilder sb = new StringBuilder();
                sb.append("      FINANCIAL STATEMENT      \n");
                sb.append("===============================\n\n");
                sb.append(String.format("User:           %s %s\n", user.getFirstName(), user.getLastName()));
                sb.append(String.format("Range:          %s to %s\n", from, to));
                if (category != null) {
                    sb.append(String.format("Category:       %s\n", category.getName()));
                }
                sb.append(String.format("Generated on:   %s\n", LocalDateTime.now().toString().replace('T', ' ')));
                sb.append("\nDETAILS:\n");
                sb.append(String.format("%-12s | %-15s | %-10s | %s\n", "Date", "Category", "Amount", "Type"));
                sb.append("-------------------------------------------------------------\n");

                for (int i = 0; i < totalCount; i++) {
                    if (isCancelled()) {
                        updateMessage("Cancelled.");
                        return null;
                    }
                    Transaction t = transactions.get(i);
                    sb.append(String.format("%-12s | %-15s | $%9.2f | %s\n", 
                        t.getDate(), 
                        t.getCategory().getName(), 
                        t.getAmount(), 
                        t.getType()));
                    
                    if ("income".equalsIgnoreCase(t.getType())) {
                        income += t.getAmount();
                    } else {
                        expenses += t.getAmount();
                    }

                    updateProgress(i + 1, totalCount);
                    updateMessage(String.format("Processed %d/%d transactions...", i + 1, totalCount));
                    
                    if (totalCount > 50) Thread.sleep(2); 
                }

                sb.append("-------------------------------------------------------------\n\n");
                sb.append("SUMMARY:\n");
                sb.append(String.format("Total Income:   $%10.2f\n", income));
                sb.append(String.format("Total Expenses: $%10.2f\n", expenses));
                sb.append(String.format("Net Balance:    $%10.2f\n", income - expenses));
                sb.append("\n===============================\n");
                sb.append("   End of Financial Statement  \n");

                updateMessage("Completed successfully.");
                return sb.toString();
            }
        };

        // Declarative Bindings for Pitfall Protection
        statusLabel.textProperty().bind(currentTask.messageProperty());
        loadingIndicator.progressProperty().bind(currentTask.progressProperty());
        loadingIndicator.visibleProperty().bind(currentTask.runningProperty());
        cancelButton.disableProperty().bind(currentTask.runningProperty().not());
        
        generateButton.disableProperty().bind(currentTask.runningProperty());
        fromDatePicker.disableProperty().bind(currentTask.runningProperty());
        toDatePicker.disableProperty().bind(currentTask.runningProperty());
        categoryComboBox.disableProperty().bind(currentTask.runningProperty());

        currentTask.setOnSucceeded(e -> {
            statementTextArea.setText(currentTask.getValue());
            unbindTask();
        });

        currentTask.setOnFailed(e -> {
            unbindTask();
            // Alert shown on UI thread via onFailed
            WindowManager.showErrorAlert("Generation Error", "An error occurred while calculating the report.");
            currentTask.getException().printStackTrace();
        });

        currentTask.setOnCancelled(e -> {
            unbindTask();
            WindowManager.showInfoAlert("Cancelled", "Statement generation was stopped by user.");
        });

        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void unbindTask() {
        statusLabel.textProperty().unbind();
        loadingIndicator.progressProperty().unbind();
        loadingIndicator.visibleProperty().unbind();
        cancelButton.disableProperty().unbind();
        generateButton.disableProperty().unbind();
        fromDatePicker.disableProperty().unbind();
        toDatePicker.disableProperty().unbind();
        categoryComboBox.disableProperty().unbind();
        
        loadingIndicator.setVisible(false);
        cancelButton.setDisable(true);
        generateButton.setDisable(false);
        fromDatePicker.setDisable(false);
        toDatePicker.setDisable(false);
        categoryComboBox.setDisable(false);
    }

    @FXML
    protected void onCancelButtonClick() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    private void configureCategoryComboBoxDisplay() {
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "All Categories" : category.getName();
            }
            @Override
            public Category fromString(String string) { return null; }
        });
    }
}
