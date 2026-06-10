package repository;

import jakarta.persistence.EntityManager;
import model.User;
import service.DatabaseConfig;
import java.util.Locale;

public class UserRepository {

    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                    .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }
    }

    public void save(User user) {
        EntityManager em = DatabaseConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            if (user.getId() == 0) {
                em.persist(user);
            } else {
                em.merge(user);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
