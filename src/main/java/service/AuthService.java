package service;

import jakarta.persistence.EntityManager;
import model.Category;
import model.User;

import util.HashUtil;

import java.util.List;
import java.util.Locale;

public class AuthService {
    private static final String[] DEFAULT_CATEGORIES = {"Food", "Salary", "Transport", "Shopping", "Education"};

    /**
     * Registers a new user by validating input, checking email uniqueness, hashing the password,
     * and creating the user record in the database.
     *
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param email the user's email address
     * @param rawPassword the user's plain-text password before hashing
     * @return true when the user record is created successfully
     * @throws IllegalArgumentException if any required input is blank or the email already exists
     */
    public boolean createUser(String firstName, String lastName, String email, String rawPassword) {
        if (isBlank(firstName) || isBlank(lastName) || isBlank(email) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("First name, last name, email and password are required");
        }
        if (emailExists(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String hashed = HashUtil.md5(rawPassword);
        createUserInDatabase(firstName, lastName, email, hashed);
        return true;
    }

    /**
     * Validates user login credentials against stored user records.
     *
     * @param email the email to authenticate
     * @param rawPassword the plain-text password to verify
     * @return true when email exists and password hash matches; false when credentials do not match
     * @throws IllegalArgumentException if email or password is blank
     */
    public boolean validateUser(String email, String rawPassword) {
        if (isBlank(email) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String inputHash = HashUtil.md5(rawPassword);

        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            List<String> hashes = entityManager.createQuery(
                            "SELECT u.passwordHash FROM User u WHERE LOWER(u.email) = :email", String.class)
                    .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                    .setMaxResults(1)
                    .getResultList();
            if (hashes.isEmpty()) {
                return false;
            }
            String storedHash = hashes.getFirst();
            return storedHash != null && storedHash.equals(inputHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate user credentials", e);
        }
    }

    /**
     * Checks whether a user with the provided email already exists.
     *
     * @param email the email to search for
     * @return true if a matching email is found; false otherwise
     */
    private boolean emailExists(String email) {
        try (EntityManager entityManager = DatabaseConfig.createEntityManager()) {
            Long count = entityManager.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE LOWER(u.email) = :email", Long.class)
                    .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check email existence", e);
        }
    }


    private void createUserInDatabase(String firstName, String lastName, String email, String hashedPassword) {
        EntityManager entityManager = DatabaseConfig.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            User user = new User();
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());
            user.setEmail(email.trim());
            user.setPasswordHash(hashedPassword);

            for (String categoryName : DEFAULT_CATEGORIES) {
                Category category = new Category();
                category.setName(categoryName);
                user.addCategory(category);
            }

            entityManager.persist(user);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to create user in database", e);
        } finally {
            entityManager.close();
        }
    }

    /**
     * Checks whether a text value is null, empty, or whitespace-only.
     *
     * @param value the value to evaluate
     * @return true if the value is blank; false otherwise
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


}
