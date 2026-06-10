package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import model.Category;
import service.CategoryService;
import util.WindowManager;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CategoriesController {
    private static final String SORT_NAME_ASC = "Name (A-Z)";
    private static final String SORT_NAME_DESC = "Name (Z-A)";


    private final CategoryService categoryService = new CategoryService();

    @FXML
    private TextField categoryNameField;

    @FXML
    private TextField searchCategoryField;

    @FXML
    private ComboBox<String> sortCategoryComboBox;

    @FXML
    private TableView<Category> categoriesTable;

    @FXML
    private TableColumn<Category, Integer> idColumn;

    @FXML
    private TableColumn<Category, String> nameColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private javafx.scene.control.ProgressIndicator loadingIndicator;

    @FXML
    private javafx.scene.control.Button addEditCategoryButton;

    @FXML
    private javafx.scene.control.Button deleteButton;

    @FXML
    private javafx.scene.control.Button clearButton;

    private Category selectedCategory;
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private String lastCategoryNotFoundKeyword;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getId()).asObject());

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        configureSort();
        bindSearchAndSort();
        loadCategories();

        categoriesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedCategory = newValue;

            if (newValue != null) {
                categoryNameField.setText(newValue.getName());
            }
        });
    }

    @FXML
    private void onAddEditCategoryButtonClick() {
        String categoryName = categoryNameField.getText();

        try {
            if (selectedCategory == null) {
                categoryService.addCategory(categoryName);
                showMessage("Category added successfully.");
                categoryNameField.clear();
            } else {
                categoryService.updateCategory(selectedCategory.getId(), categoryName);
                showMessage("Category updated successfully.");
            }

            loadCategories();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to save category.");
        }
    }

    @FXML
    private void onClearButtonClick() {
        clearForm();
    }

    @FXML
    private void onDeleteButtonClick() {
        if (selectedCategory == null) {
            showError("Please select a category to delete.");
            return;
        }

        String deletedCategoryName = selectedCategory.getName();

        try {
            categoryService.deleteCategory(selectedCategory.getId());
            loadCategories();
            clearForm();
            showInfo("Category \"" + deletedCategoryName + "\" deleted successfully.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to delete category.");
        }
    }

    @FXML
    private void onBackButtonClick(ActionEvent event) {
        WindowManager.switchToDashboard(event);
    }

    private void bindSearchAndSort() {
        searchCategoryField.textProperty().addListener((observable, oldValue, newValue) ->
                refreshCategoriesView());

        sortCategoryComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                refreshCategoriesView());
    }

    private void configureSort() {
        sortCategoryComboBox.setItems(FXCollections.observableArrayList(SORT_NAME_ASC, SORT_NAME_DESC));
        sortCategoryComboBox.setValue(SORT_NAME_ASC);
    }

    private void refreshCategoriesView() {
        String keyword = searchCategoryField.getText() == null
                ? ""
                : searchCategoryField.getText().trim().toLowerCase(Locale.ROOT);

        Comparator<Category> comparator = Comparator.comparing(
                c -> c.getName().toLowerCase(Locale.ROOT)
        );
        if (SORT_NAME_DESC.equals(sortCategoryComboBox.getValue())) {
            comparator = comparator.reversed();
        }

        List<Category> viewData = categories.stream()
                .filter(category -> keyword.isEmpty()
                        || category.getName().toLowerCase(Locale.ROOT).contains(keyword))
                .sorted(comparator)
                .collect(Collectors.toList());

        categoriesTable.setItems(FXCollections.observableArrayList(viewData));

        if (keyword.isEmpty() || !viewData.isEmpty()) {
            lastCategoryNotFoundKeyword = null;
            return;
        }

        if (!keyword.equals(lastCategoryNotFoundKeyword)) {
            lastCategoryNotFoundKeyword = keyword;
            WindowManager.showErrorAlert("Category Not Found", "No category found with that name.");
        }
    }

    private void loadCategories() {
        javafx.concurrent.Task<List<Category>> loadTask = categoryService.getAllCategoriesTask();

        loadingIndicator.visibleProperty().bind(loadTask.runningProperty());
        addEditCategoryButton.disableProperty().bind(loadTask.runningProperty());
        deleteButton.disableProperty().bind(loadTask.runningProperty());
        clearButton.disableProperty().bind(loadTask.runningProperty());

        loadTask.setOnSucceeded(event -> {
            categories.setAll(loadTask.getValue());
            refreshCategoriesView();
            unbindTask();
        });

        loadTask.setOnFailed(event -> {
            unbindTask();
            showError("Failed to load categories.");
            loadTask.getException().printStackTrace();
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void unbindTask() {
        loadingIndicator.visibleProperty().unbind();
        addEditCategoryButton.disableProperty().unbind();
        deleteButton.disableProperty().unbind();
        clearButton.disableProperty().unbind();
        
        loadingIndicator.setVisible(false);
        addEditCategoryButton.setDisable(false);
        deleteButton.setDisable(false);
        clearButton.setDisable(false);
    }

    private void clearForm() {
        categoryNameField.clear();
        categoriesTable.getSelectionModel().clearSelection();
        selectedCategory = null;
        messageLabel.setText("");
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private void showError(String message) {
        WindowManager.showErrorAlert("Error", message);
    }

    private void showInfo(String message) {
        WindowManager.showInfoAlert("Information", message);
    }
}
