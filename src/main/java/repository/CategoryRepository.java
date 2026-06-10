package repository;

import jakarta.persistence.EntityManager;
import model.Category;
import model.User;
import service.DatabaseConfig;
import service.SessionManager;
import java.util.List;
import java.util.Locale;

public class CategoryRepository {

    public List<Category> findAll() {
        String email = SessionManager.getLoggedInUserEmail();
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT c FROM Category c WHERE LOWER(c.user.email) = :email", Category.class)
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .getResultList();
        }
    }

    public Category findByName(String name) {
        String email = SessionManager.getLoggedInUserEmail();
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT c FROM Category c WHERE LOWER(c.name) = :name AND LOWER(c.user.email) = :email", Category.class)
                    .setParameter("name", name.trim().toLowerCase(Locale.ROOT))
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }
    }

    public void save(Category category) {
        EntityManager em = DatabaseConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            if (category.getId() == 0) {
                em.persist(category);
            } else {
                em.merge(category);
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

    public void delete(int id) {
        EntityManager em = DatabaseConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            Category category = em.find(Category.class, id);
            if (category != null) {
                em.remove(category);
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
