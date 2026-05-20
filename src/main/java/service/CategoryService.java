package service;

import jakarta.persistence.EntityManager;
import model.Category;
import model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CategoryService {

    public List<Category> getAllCategories() {
        String email = SessionManager.getLoggedInUserEmail();
        if (email == null || email.trim().isEmpty()) {
            return List.of();
        }

        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            User user = getUserWithCategories(entityManager, email);
            if (user == null) {
                return List.of();
            }
            List<Category> categories = new ArrayList<>(user.getCategories());
            categories.sort(Comparator.comparingInt(Category::getId));
            return categories;
        }
    }

    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        String email = SessionManager.getLoggedInUserEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalStateException("No logged-in user found.");
        }

        String trimmedName = name.trim();
        String normalizedInput = normalizeName(trimmedName);
        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            User user = getUserWithCategories(entityManager, email);
            if (user == null) {
                throw new IllegalStateException("No logged-in user found.");
            }

            boolean exists = user.getCategories().stream()
                    .map(Category::getName)
                    .map(this::normalizeName)
                    .anyMatch(normalizedInput::equals);
            if (exists) {
                throw new IllegalArgumentException("Category already exists.");
            }

            Category category = new Category();
            category.setName(trimmedName);
            user.addCategory(category);
            entityManager.getTransaction().commit();
        } catch (IllegalArgumentException e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save category.", e);
        } finally {
            entityManager.close();
        }
    }

    public void updateCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        String trimmedName = newName.trim();
        String normalizedInput = normalizeName(trimmedName);
        String email = SessionManager.getLoggedInUserEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalStateException("No logged-in user found.");
        }

        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            User user = getUserWithCategories(entityManager, email);
            if (user == null) {
                throw new IllegalStateException("No logged-in user found.");
            }

            Category category = user.getCategories().stream()
                    .filter(c -> c.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (category == null) {
                throw new IllegalArgumentException("Category not found.");
            }

            boolean exists = user.getCategories().stream()
                    .filter(c -> c.getId() != id)
                    .map(Category::getName)
                    .map(this::normalizeName)
                    .anyMatch(normalizedInput::equals);
            if (exists) {
                throw new IllegalArgumentException("Category already exists.");
            }

            category.setName(trimmedName);
            entityManager.getTransaction().commit();
        } catch (IllegalArgumentException e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to update category.", e);
        } finally {
            entityManager.close();
        }
    }

    public void deleteCategory(int id) {
        String email = SessionManager.getLoggedInUserEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalStateException("No logged-in user found.");
        }

        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            User user = getUserWithCategories(entityManager, email);
            if (user == null) {
                throw new IllegalStateException("No logged-in user found.");
            }

            Category category = user.getCategories().stream()
                    .filter(c -> c.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (category == null) {
                throw new IllegalArgumentException("Category not found.");
            }
            if (!category.getTransactions().isEmpty()) {
                throw new IllegalArgumentException("Cannot delete category with existing transactions.");
            }
            user.getCategories().remove(category);
            entityManager.getTransaction().commit();
        } catch (IllegalArgumentException e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete category.", e);
        } finally {
            entityManager.close();
        }
    }

    private User getUserWithCategories(EntityManager entityManager, String email) {
        return entityManager.createQuery("""
                        SELECT DISTINCT u
                        FROM User u
                        LEFT JOIN FETCH u.categories
                        WHERE LOWER(u.email) = :email
                        """, User.class)
                .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
