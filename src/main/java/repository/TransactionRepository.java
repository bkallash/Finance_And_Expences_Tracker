package repository;

import jakarta.persistence.EntityManager;
import model.Category;
import model.Transaction;
import model.User;
import service.DatabaseConfig;
import service.SessionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TransactionRepository {

    public List<Transaction> findAll() {
        String email = SessionManager.getLoggedInUserEmail();
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT t FROM Transaction t WHERE LOWER(t.user.email) = :email", Transaction.class)
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .getResultList();
        }
    }

    public List<Transaction> findByUser(User user) {
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT t FROM Transaction t WHERE t.user.id = :userId", Transaction.class)
                    .setParameter("userId", user.getId())
                    .getResultList();
        }
    }

    public List<Transaction> findByCategory(Category category) {
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT t FROM Transaction t WHERE t.category.id = :categoryId", Transaction.class)
                    .setParameter("categoryId", category.getId())
                    .getResultList();
        }
    }

    public List<Transaction> findByType(String type) {
        String email = SessionManager.getLoggedInUserEmail();
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT t FROM Transaction t WHERE LOWER(t.type) = :type AND LOWER(t.user.email) = :email", Transaction.class)
                    .setParameter("type", type.toLowerCase(Locale.ROOT))
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .getResultList();
        }
    }

    public List<Transaction> findByDateRange(LocalDate from, LocalDate to) {
        String email = SessionManager.getLoggedInUserEmail();
        try (EntityManager em = DatabaseConfig.createEntityManager()) {
            return em.createQuery("SELECT t FROM Transaction t WHERE t.transactionDate >= :from AND t.transactionDate <= :to AND LOWER(t.user.email) = :email", Transaction.class)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .getResultList();
        }
    }

    public void save(Transaction transaction) {
        EntityManager em = DatabaseConfig.createEntityManager();
        try {
            em.getTransaction().begin();
            if (transaction.getId() == 0) {
                em.persist(transaction);
            } else {
                em.merge(transaction);
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
            Transaction transaction = em.find(Transaction.class, id);
            if (transaction != null) {
                em.remove(transaction);
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
