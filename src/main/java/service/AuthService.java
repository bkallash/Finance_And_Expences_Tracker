package service;

import model.Category;
import model.User;
import repository.UserRepository;
import util.HashUtil;

public class AuthService {
    private static final String[] DEFAULT_CATEGORIES = {"Food", "Salary", "Transport", "Shopping", "Education"};
    private final UserRepository userRepository = new UserRepository();

    public boolean createUser(String firstName, String lastName, String email, String rawPassword) {
        if (isBlank(firstName) || isBlank(lastName) || isBlank(email) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("First name, last name, email and password are required");
        }
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(HashUtil.md5(rawPassword));

        for (String categoryName : DEFAULT_CATEGORIES) {
            Category category = new Category();
            category.setName(categoryName);
            user.addCategory(category);
        }

        userRepository.save(user);
        return true;
    }

    public boolean validateUser(String email, String rawPassword) {
        if (isBlank(email) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("Email and password are required");
        }
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        String inputHash = HashUtil.md5(rawPassword);
        return user.getPasswordHash() != null && user.getPasswordHash().equals(inputHash);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
