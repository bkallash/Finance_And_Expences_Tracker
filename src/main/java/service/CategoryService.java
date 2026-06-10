package service;

import javafx.concurrent.Task;
import model.Category;
import model.User;
import repository.CategoryRepository;
import service.SessionManager;
import service.UserService;

import java.util.List;

public class CategoryService {
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final UserService userService = new UserService();

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a Task to load all categories asynchronously.
     * @return A Task that returns a List of Categories.
     */
    public Task<List<Category>> getAllCategoriesTask() {
        return new Task<>() {
            @Override
            protected List<Category> call() throws Exception {
                return getAllCategories();
            }
        };
    }

    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        if (categoryRepository.findByName(name) != null) {
            throw new IllegalArgumentException("Category with this name already exists.");
        }

        User user = userService.getUserByEmail(SessionManager.getLoggedInUserEmail());
        Category category = new Category();
        category.setName(name.trim());
        category.setUser(user);
        categoryRepository.save(category);
    }

    public void updateCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        Category category = categoryRepository.findByName(newName);
        if (category != null && category.getId() != id) {
            throw new IllegalArgumentException("Another category with this name already exists.");
        }

        Category toUpdate = new Category();
        toUpdate.setId(id);
        toUpdate.setName(newName.trim());
        toUpdate.setUser(userService.getUserByEmail(SessionManager.getLoggedInUserEmail()));
        categoryRepository.save(toUpdate);
    }

    public void deleteCategory(int id) {
        categoryRepository.delete(id);
    }
}
