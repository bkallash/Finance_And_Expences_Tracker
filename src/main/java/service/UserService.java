package service;

import jakarta.persistence.EntityManager;
import model.User;

import java.util.List;
import java.util.Locale;

public class UserService {
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            List<User> users = entityManager.createQuery(
                            "SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                    .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                    .setMaxResults(1)
                    .getResultList();
            return users.isEmpty() ? null : users.getFirst();
        }
    }

    public boolean updateUser(User user) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Valid user is required");
        }

        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            entityManager.getTransaction().begin();
            User managed = entityManager.find(User.class, user.getId());
            if (managed == null) {
                entityManager.getTransaction().rollback();
                return false;
            }
            managed.setFirstName(user.getFirstName());
            managed.setLastName(user.getLastName());
            managed.setEmail(user.getEmail());
            managed.setPasswordHash(user.getPasswordHash());
            entityManager.getTransaction().commit();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public boolean deleteUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            entityManager.getTransaction().begin();
            List<User> users = entityManager.createQuery(
                            "SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                    .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                    .setMaxResults(1)
                    .getResultList();
            if (users.isEmpty()) {
                entityManager.getTransaction().rollback();
                return false;
            }
            entityManager.remove(users.getFirst());
            entityManager.getTransaction().commit();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }
}
