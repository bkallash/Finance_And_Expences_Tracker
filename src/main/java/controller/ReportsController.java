package controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Category;
import model.Transaction;
import model.User;
import service.CategoryService;
import service.SessionManager;
import service.TransactionService;
import service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportsController {

    @FXML
    private ComboBox<String> reportTypeComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Button generateButton;

    @FXML
    private Button cancelButton;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea reportTextArea;

    private final TransactionService transactionService = new TransactionService();
    private final CategoryService categoryService = new CategoryService();
    private final UserService userService = new UserService();

    private Task<String> currentTask;

    private static final String REPORT_MONTHLY_INCOME = "Monthly Income";
    private static final String REPORT_MONTHLY_EXPENSES = "Monthly Expenses";
    private static final String REPORT_CATEGORY_SPENDING = "Category Spending";
    private static final String REPORT_BALANCE_SUMMARY = "Balance Summary";

    @FXML
    public void initialize() {
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
                REPORT_MONTHLY_INCOME,
                REPORT_MONTHLY_EXPENSES,
                REPORT_CATEGORY_SPENDING,
                REPORT_BALANCE_SUMMARY
        ));

        configureCategoryComboBox();
        loadCategories();

        // Default dates: current month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());
    }

    private void configureCategoryComboBox() {
        categoryComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "All Categories" : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null; // Not needed
            }
        });
    }

    private void loadCategories() {
        Task<List<Category>> loadTask = categoryService.getAllCategoriesTask();
        loadTask.setOnSucceeded(e -> {
            categoryComboBox.setItems(FXCollections.observableArrayList(loadTask.getValue()));
        });
        new Thread(loadTask).start();
    }

    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        Category selectedCategory = categoryComboBox.getValue();

        if (reportType == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Filter", "Please select a report type.");
            return;
        }

        if (fromDate == null || toDate == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Filter", "Please select both from and to dates.");
            return;
        }

        if (fromDate.isAfter(toDate)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Filter", "From date cannot be after to date.");
            return;
        }

        generateReport(reportType, fromDate, toDate, selectedCategory);
    }

    private void generateReport(String type, LocalDate from, LocalDate to, Category category) {
        progressIndicator.setVisible(true);
        statusLabel.setText("Generating report...");
        generateButton.setDisable(true);
        reportTextArea.clear();

        currentTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                User user = userService.getUserByEmail(SessionManager.getLoggedInUserEmail());
                List<Transaction> transactions = transactionService.getTransactionsByCriteria(user, from, to, category);

                if (transactions.isEmpty()) {
                    return "No data found for the selected criteria.";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("--------------------------------------------------\n");
                sb.append("FINANCIAL REPORT: ").append(type.toUpperCase()).append("\n");
                sb.append("Period: ").append(from).append(" to ").append(to).append("\n");
                if (category != null) {
                    sb.append("Category: ").append(category.getName()).append("\n");
                }
                sb.append("Generated on: ").append(LocalDate.now()).append("\n");
                sb.append("--------------------------------------------------\n\n");

                switch (type) {
                    case REPORT_MONTHLY_INCOME:
                        generateMonthlyReport(sb, transactions, "income");
                        break;
                    case REPORT_MONTHLY_EXPENSES:
                        generateMonthlyReport(sb, transactions, "expense");
                        break;
                    case REPORT_CATEGORY_SPENDING:
                        generateCategorySpendingReport(sb, transactions);
                        break;
                    case REPORT_BALANCE_SUMMARY:
                        generateBalanceSummaryReport(sb, transactions);
                        break;
                }

                return sb.toString();
            }
        };

        currentTask.setOnSucceeded(e -> {
            reportTextArea.setText(currentTask.getValue());
            finishTask("Report generated successfully.");
        });

        currentTask.setOnFailed(e -> {
            finishTask("Failed to generate report.");
            showAlert(Alert.AlertType.ERROR, "Generation Failure", "An error occurred while generating the report: " + currentTask.getException().getMessage());
        });

        currentTask.setOnCancelled(e -> {
            finishTask("Report generation cancelled.");
        });

        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void generateMonthlyReport(StringBuilder sb, List<Transaction> transactions, String type) {
        List<Transaction> filtered = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase(type))
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            sb.append("No ").append(type).append(" transactions found.\n");
            return;
        }

        sb.append(String.format("%-12s | %-20s | %10s\n", "Date", "Category", "Amount"));
        sb.append("--------------------------------------------------\n");

        double total = 0;
        for (Transaction t : filtered) {
            sb.append(String.format("%-12s | %-20s | %10.2f\n",
                    t.getTransactionDate(),
                    t.getCategory().getName(),
                    t.getAmount()));
            total += t.getAmount();
        }

        sb.append("--------------------------------------------------\n");
        sb.append(String.format("TOTAL %-7s: %27.2f\n", type.toUpperCase(), total));
    }

    private void generateCategorySpendingReport(StringBuilder sb, List<Transaction> transactions) {
        Map<String, Double> spendingByCategory = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("expense"))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        if (spendingByCategory.isEmpty()) {
            sb.append("No expense transactions found.\n");
            return;
        }

        sb.append(String.format("%-30s | %15s\n", "Category", "Total Spent"));
        sb.append("--------------------------------------------------\n");

        double total = 0;
        for (Map.Entry<String, Double> entry : spendingByCategory.entrySet()) {
            sb.append(String.format("%-30s | %15.2f\n", entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }

        sb.append("--------------------------------------------------\n");
        sb.append(String.format("TOTAL SPENDING: %34.2f\n", total));
    }

    private void generateBalanceSummaryReport(StringBuilder sb, List<Transaction> transactions) {
        double income = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("income"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("expense"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        sb.append(String.format("Total Income:   %20.2f\n", income));
        sb.append(String.format("Total Expenses: %20.2f\n", expense));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("NET BALANCE:    %20.2f\n", income - expense));
    }

    @FXML
    private void handleCancelReport() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    private void finishTask(String message) {
        progressIndicator.setVisible(false);
        statusLabel.setText(message);
        generateButton.setDisable(false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
