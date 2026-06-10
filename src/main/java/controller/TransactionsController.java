package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionsController {
    private static final String SORT_DATE_NEWEST = "Date (Newest)";
    private static final String SORT_DATE_OLDEST = "Date (Oldest)";
    private static final String SORT_AMOUNT_LOW_HIGH = "Amount (Low-High)";
    private static final String SORT_AMOUNT_HIGH_LOW = "Amount (High-Low)";

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private DatePicker transactionDatePicker;

    @FXML
    private TextField searchTransactionField;

    @FXML
    private ComboBox<String> sortTransactionComboBox;

    @FXML
    private TableView<Transaction> transactionsTable;

    @FXML
    private TableColumn<Transaction, Integer> idColumn;

    @FXML
    private TableColumn<Transaction, String> categoryNameColumn;

    @FXML
    private TableColumn<Transaction, Double> amountColumn;

    @FXML
    private TableColumn<Transaction, String> typeColumn;

    @FXML
    private TableColumn<Transaction, String> dateColumn;

    @FXML
    private Button addEditTransactionButton;

    @FXML
    private Button deleteTransactionButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button summaryButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button todayButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button backButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    private final TransactionService transactionService = new TransactionService();
    private final CategoryService categoryService = new CategoryService();
    private final UserService userService = new UserService();

    private Transaction selectedTransaction;
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private String lastSearchNotFoundKeyword;
    private Task<?> currentTask;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        categoryNameColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(
                        cellData.getValue().getCategory() == null ? "" : cellData.getValue().getCategory().getName()));

        loadCategoriesAsync();
        configureCategoryComboBoxDisplay();
        typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));

        configureSort();
        bindSearchAndSort();
        loadTransactions();

        transactionsTable.setOnMouseClicked(event -> {
            Transaction transaction = transactionsTable.getSelectionModel().getSelectedItem();
            if (transaction != null) {
                selectedTransaction = transaction;
                amountField.setText(String.valueOf(transaction.getAmount()));
                categoryComboBox.setValue(transaction.getCategory());
                typeComboBox.setValue(transaction.getType());
                transactionDatePicker.setValue(LocalDate.parse(transaction.getDate()));
            }
        });
    }

    private void loadCategoriesAsync() {
        Task<List<Category>> loadCatsTask = categoryService.getAllCategoriesTask();

        loadCatsTask.setOnSucceeded(event -> {
            categoryComboBox.setItems(FXCollections.observableArrayList(loadCatsTask.getValue()));
        });

        new Thread(loadCatsTask).start();
    }

    private void startBackgroundTask(Task<?> task) {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
        currentTask = task;

        statusLabel.textProperty().bind(task.messageProperty());
        loadingIndicator.progressProperty().bind(task.progressProperty());
        loadingIndicator.visibleProperty().bind(task.runningProperty());
        cancelButton.disableProperty().bind(task.runningProperty().not());
        
        refreshButton.disableProperty().bind(task.runningProperty());
        summaryButton.disableProperty().bind(task.runningProperty());
        addEditTransactionButton.disableProperty().bind(task.runningProperty());
        deleteTransactionButton.disableProperty().bind(task.runningProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void unbindTask() {
        statusLabel.textProperty().unbind();
        loadingIndicator.progressProperty().unbind();
        loadingIndicator.visibleProperty().unbind();
        cancelButton.disableProperty().unbind();
        refreshButton.disableProperty().unbind();
        summaryButton.disableProperty().unbind();
        addEditTransactionButton.disableProperty().unbind();
        deleteTransactionButton.disableProperty().unbind();
        
        loadingIndicator.setVisible(false);
        cancelButton.setDisable(true);
        refreshButton.setDisable(false);
        summaryButton.setDisable(false);
        addEditTransactionButton.setDisable(false);
        deleteTransactionButton.setDisable(false);
    }

    private void loadTransactions() {
        Task<List<Transaction>> loadTask = transactionService.getAllTransactionsTask();

        loadTask.setOnSucceeded(event -> {
            transactions.setAll(loadTask.getValue());
            refreshTransactionsView();
            unbindTask();
            statusLabel.setText("Transactions loaded.");
        });

        loadTask.setOnFailed(event -> {
            unbindTask();
            WindowManager.showErrorAlert("Error", "Failed to load transactions.");
            statusLabel.setText("Load failed.");
        });

        loadTask.setOnCancelled(event -> {
            unbindTask();
            statusLabel.setText("Load cancelled.");
        });

        startBackgroundTask(loadTask);
    }

    @FXML
    protected void onAddEditTransactionButtonClick() {
        try {
            String email = SessionManager.getLoggedInUserEmail();
            User user = userService.getUserByEmail(email);

            if (user == null) {
                WindowManager.showErrorAlert("Input Error", "No logged-in user found.");
                return;
            }

            double amount = Double.parseDouble(amountField.getText().trim());
            Category category = categoryComboBox.getValue();
            String type = typeComboBox.getValue();
            LocalDate date = transactionDatePicker.getValue();

            if (category == null || type == null || date == null) {
                WindowManager.showErrorAlert("Input Error", "Please complete all fields.");
                return;
            }

            if (amount <= 0) {
                WindowManager.showErrorAlert("Input Error", "Amount must be greater than zero.");
                return;
            }

            if (selectedTransaction == null) {
                transactionService.addTransaction(user, category, amount, type, date.toString());
                WindowManager.showInfoAlert("Success", "Transaction added successfully.");
            } else {
                transactionService.updateTransaction(selectedTransaction.getId(), user, category, amount, type, date.toString());
                WindowManager.showInfoAlert("Success", "Transaction updated successfully.");
            }

            clearForm();
            loadTransactions();

        } catch (NumberFormatException e) {
            WindowManager.showErrorAlert("Input Error", "Amount must be a valid number.");
        } catch (Exception e) {
            String message = e.getMessage();
            WindowManager.showErrorAlert("Input Error",
                    (message == null || message.isBlank()) ? "Unable to save transaction." : message);
        }
    }

    @FXML
    protected void onClearButtonClick() {
        clearForm();
    }

    @FXML
    protected void onDeleteTransactionButtonClick() {
        if (selectedTransaction == null) {
            WindowManager.showErrorAlert("Input Error", "Please select a transaction to delete.");
            return;
        }

        try {
            String email = SessionManager.getLoggedInUserEmail();
            User user = userService.getUserByEmail(email);

            if (user == null) {
                WindowManager.showErrorAlert("Input Error", "No logged-in user found.");
                return;
            }

            int transactionId = selectedTransaction.getId();
            transactionService.deleteTransaction(transactionId, user);
            clearForm();
            loadTransactions();
            WindowManager.showInfoAlert("Success", "Transaction deleted successfully.");
        } catch (IllegalArgumentException e) {
            WindowManager.showErrorAlert("Input Error", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            WindowManager.showErrorAlert("Input Error",
                    (message == null || message.isBlank()) ? "Unable to delete transaction." : message);
        }
    }

    @FXML
    protected void onRefreshButtonClick() {
        loadTransactions();
    }

    @FXML
    protected void onGenerateSummaryButtonClick() {
        if (transactions.isEmpty()) {
            WindowManager.showInfoAlert("Summary", "No transactions to summarize.");
            return;
        }

        Task<String> summaryTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Generating summary...");
                Map<String, List<Transaction>> grouped = transactions.stream()
                        .collect(Collectors.groupingBy(t -> t.getCategory().getName()));

                StringBuilder sb = new StringBuilder("Category Summary:\n\n");
                int totalCats = grouped.size();
                int currentCat = 0;

                for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
                    if (isCancelled()) {
                        updateMessage("Cancelled.");
                        return null;
                    }
                    String catName = entry.getKey();
                    List<Transaction> list = entry.getValue();
                    double income = list.stream().filter(t -> "income".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
                    double expense = list.stream().filter(t -> "expense".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
                    
                    sb.append(String.format("- %s:\n  Income: $%.2f | Expense: $%.2f | Net: $%.2f\n\n", 
                        catName, income, expense, income - expense));
                    
                    currentCat++;
                    updateProgress(currentCat, totalCats);
                    updateMessage(String.format("Summarized %d/%d categories...", currentCat, totalCats));
                    Thread.sleep(200); 
                }
                updateMessage("Summary complete.");
                return sb.toString();
            }
        };

        summaryTask.setOnSucceeded(e -> {
            unbindTask();
            WindowManager.showInfoAlert("Category Summary Result", summaryTask.getValue());
        });

        summaryTask.setOnFailed(e -> {
            unbindTask();
            WindowManager.showErrorAlert("Error", "Summary generation failed.");
        });

        summaryTask.setOnCancelled(e -> {
            unbindTask();
            WindowManager.showInfoAlert("Cancelled", "Operation was cancelled.");
        });

        startBackgroundTask(summaryTask);
    }

    @FXML
    protected void onCancelButtonClick() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    @FXML
    protected void onTodayButtonClick() {
        transactionDatePicker.setValue(LocalDate.now());
    }

    @FXML
    protected void onBackButtonClick(ActionEvent event) {
        WindowManager.switchToDashboard(event);
    }

    private void bindSearchAndSort() {
        if (searchTransactionField != null) {
            searchTransactionField.textProperty().addListener((observable, oldValue, newValue) ->
                    refreshTransactionsView());
        }

        sortTransactionComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                refreshTransactionsView());
    }

    private void configureSort() {
        sortTransactionComboBox.setItems(FXCollections.observableArrayList(
                SORT_DATE_NEWEST,
                SORT_DATE_OLDEST,
                SORT_AMOUNT_LOW_HIGH,
                SORT_AMOUNT_HIGH_LOW
        ));
        sortTransactionComboBox.setValue(SORT_DATE_NEWEST);
    }

    private void refreshTransactionsView() {
        String keyword = searchTransactionField.getText() == null
                ? ""
                : searchTransactionField.getText().trim().toLowerCase(Locale.ROOT);

        Comparator<Transaction> comparator = buildTransactionComparator(sortTransactionComboBox.getValue());
        boolean categoryOrDateMatchExists = hasCategoryOrDateMatch(keyword);

        List<Transaction> viewData = transactions.stream()
                .filter(transaction -> matchesTransactionKeyword(transaction, keyword))
                .sorted(comparator)
                .collect(Collectors.toList());

        transactionsTable.setItems(FXCollections.observableArrayList(viewData));

        if (keyword.isEmpty() || categoryOrDateMatchExists) {
            lastSearchNotFoundKeyword = null;
            return;
        }

        if (!keyword.equals(lastSearchNotFoundKeyword)) {
            lastSearchNotFoundKeyword = keyword;
            WindowManager.showErrorAlert("Search Not Found", "No transaction category or date found for that search.");
        }
    }

    private Comparator<Transaction> buildTransactionComparator(String sortOption) {
        if (SORT_DATE_OLDEST.equals(sortOption)) {
            return Comparator.comparing(t -> LocalDate.parse(t.getDate()));
        }

        if (SORT_AMOUNT_LOW_HIGH.equals(sortOption)) {
            return Comparator.comparingDouble(Transaction::getAmount);
        }

        if (SORT_AMOUNT_HIGH_LOW.equals(sortOption)) {
            return Comparator.comparingDouble(Transaction::getAmount).reversed();
        }

        return Comparator.comparing((Transaction t) -> LocalDate.parse(t.getDate())).reversed();
    }

    private boolean matchesTransactionKeyword(Transaction transaction, String keyword) {
        if (transaction == null) {
            return false;
        }
        if (keyword.isEmpty()) {
            return true;
        }

        String categoryName = transaction.getCategory() == null || transaction.getCategory().getName() == null
                ? ""
                : transaction.getCategory().getName().toLowerCase(Locale.ROOT);
        String date = transaction.getDate() == null ? "" : transaction.getDate().toLowerCase(Locale.ROOT);

        return categoryName.contains(keyword) || date.contains(keyword);
    }

    private boolean hasCategoryOrDateMatch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        return transactions.stream().anyMatch(transaction -> {
            if (transaction == null || transaction.getCategory() == null || transaction.getCategory().getName() == null) {
                String date = transaction == null || transaction.getDate() == null
                        ? ""
                        : transaction.getDate().toLowerCase(Locale.ROOT);
                return date.contains(keyword);
            }

            String categoryName = transaction.getCategory().getName().toLowerCase(Locale.ROOT);
            String date = transaction.getDate() == null
                    ? ""
                    : transaction.getDate().toLowerCase(Locale.ROOT);

            return categoryName.contains(keyword) || date.contains(keyword);
        });
    }

    private void clearForm() {
        amountField.clear();
        categoryComboBox.setValue(null);
        typeComboBox.setValue(null);
        transactionDatePicker.setValue(null);
        selectedTransaction = null;
        transactionsTable.getSelectionModel().clearSelection();
    }

    private void configureCategoryComboBoxDisplay() {
        categoryComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatCategory(item));
            }
        });

        categoryComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatCategory(item));
            }
        });

        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : formatCategory(category);
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });
    }

    private String formatCategory(Category category) {
        return category.getId() + " - " + category.getName();
    }

}
