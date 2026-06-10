package service;

import javafx.concurrent.Task;
import model.User;
import repository.UserRepository;

public class UserService {
    private final UserRepository userRepository = new UserRepository();

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Creates a Task to fetch a user by email asynchronously.
     */
    public Task<User> getUserByEmailTask(String email) {
        return new Task<>() {
            @Override
            protected User call() throws Exception {
                return getUserByEmail(email);
            }
        };
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
