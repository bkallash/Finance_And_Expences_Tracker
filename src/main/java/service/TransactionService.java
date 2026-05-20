package service;

import jakarta.persistence.EntityManager;
import model.Category;
import model.Transaction;
import model.User;

import java.util.List;
import java.util.Locale;

public class TransactionService {
    private final UserService userService = new UserService();

    public List<Transaction> getAllTransactions() {
        String email = SessionManager.getLoggedInUserEmail();
        if (email == null || email.trim().isEmpty()) {
            return List.of();
        }

        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            User user = getUserWithTransactions(entityManager, email);
            if (user == null) {
                return List.of();
            }
            return user.getTransactions().stream()
                    .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                    .toList();
        }
    }

    public void addTransaction(User user, Category category, double amount, String type, String date) {
        validateTransactionData(user, category, amount, type, date);

        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            User managedUser = entityManager.find(User.class, user.getId());
            if (managedUser == null) {
                throw new IllegalStateException("No logged-in user found.");
            }
            Transaction transaction = new Transaction();
            transaction.setCategory(entityManager.getReference(Category.class, category.getId()));
            transaction.setAmount(amount);
            transaction.setType(type.trim());
            transaction.setDate(date.trim());
            managedUser.addTransaction(transaction);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save transaction.", e);
        } finally {
            entityManager.close();
        }
    }

    public void updateTransaction(int id, User user, Category category, double amount, String type, String date) {
        validateTransactionData(user, category, amount, type, date);

        if (user == null || user.getId() <= 0) {
            throw new IllegalStateException("No logged-in user found.");
        }

        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            String email = SessionManager.getLoggedInUserEmail();
            User managedUser = getUserWithTransactions(entityManager, email);
            if (managedUser == null || managedUser.getId() != user.getId()) {
                throw new IllegalStateException("No logged-in user found.");
            }

            Transaction transaction = managedUser.getTransactions().stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction not found.");
            }
            transaction.setCategory(entityManager.getReference(Category.class, category.getId()));
            transaction.setAmount(amount);
            transaction.setType(type.trim());
            transaction.setDate(date.trim());
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
            throw new RuntimeException("Failed to update transaction.", e);
        } finally {
            entityManager.close();
        }
    }

    public void deleteTransaction(int id, User user) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalStateException("No logged-in user found.");
        }

        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            String email = SessionManager.getLoggedInUserEmail();
            User managedUser = getUserWithTransactions(entityManager, email);
            if (managedUser == null || managedUser.getId() != user.getId()) {
                throw new IllegalStateException("No logged-in user found.");
            }

            Transaction transaction = managedUser.getTransactions().stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction not found.");
            }

            managedUser.getTransactions().remove(transaction);
            entityManager.remove(transaction);
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
            throw new RuntimeException("Failed to delete transaction.", e);
        } finally {
            entityManager.close();
        }
    }

    public double getTotalIncome() {
        User user = getCurrentUser();
        if (user == null) {
            return 0;
        }
        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            Double total = entityManager.createQuery("""
                            SELECT COALESCE(SUM(t.amount), 0)
                            FROM Transaction t
                            WHERE t.user.id = :userId AND LOWER(t.type) = :type
                            """, Double.class)
                    .setParameter("userId", user.getId())
                    .setParameter("type", "income")
                    .getSingleResult();
            return total == null ? 0 : total;
        }
    }

    public double getTotalExpenses() {
        User user = getCurrentUser();
        if (user == null) {
            return 0;
        }
        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            Double total = entityManager.createQuery("""
                            SELECT COALESCE(SUM(t.amount), 0)
                            FROM Transaction t
                            WHERE t.user.id = :userId AND LOWER(t.type) = :type
                            """, Double.class)
                    .setParameter("userId", user.getId())
                    .setParameter("type", "expense")
                    .getSingleResult();
            return total == null ? 0 : total;
        }
    }

    private void validateTransactionData(User user, Category category, double amount, String type, String date) {
        if (user == null || category == null || type == null || date == null) {
            throw new IllegalArgumentException("All transaction fields are required.");
        }

        if (type.trim().isEmpty() || date.trim().isEmpty()) {
            throw new IllegalArgumentException("All transaction fields are required.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        if (!normalizedType.equals("income") && !normalizedType.equals("expense")) {
            throw new IllegalArgumentException("Type must be either Income or Expense.");
        }
    }

    private User getCurrentUser() {
        String email = SessionManager.getLoggedInUserEmail();

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return userService.getUserByEmail(email);
    }

    private User getUserWithTransactions(EntityManager entityManager, String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return entityManager.createQuery("""
                        SELECT DISTINCT u
                        FROM User u
                        LEFT JOIN FETCH u.transactions t
                        LEFT JOIN FETCH t.category
                        WHERE LOWER(u.email) = :email
                        """, User.class)
                .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
