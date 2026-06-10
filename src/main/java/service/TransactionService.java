package service;

import javafx.concurrent.Task;
import model.Category;
import model.Transaction;
import model.User;
import repository.TransactionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TransactionService {
    private final TransactionRepository transactionRepository = new TransactionRepository();

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Creates a Task to load all transactions asynchronously.
     * @return A Task that returns a List of Transactions.
     */
    public Task<List<Transaction>> getAllTransactionsTask() {
        return new Task<>() {
            @Override
            protected List<Transaction> call() throws Exception {
                return getAllTransactions();
            }
        };
    }

    public List<Transaction> getTransactionsByCriteria(User user, LocalDate from, LocalDate to, Category category) {
        List<Transaction> transactions = transactionRepository.findByDateRange(from, to);
        if (category != null) {
            return transactions.stream()
                    .filter(t -> t.getCategory().getId() == category.getId())
                    .toList();
        }
        return transactions;
    }

    /**
     * Creates a Task to load transactions by criteria asynchronously.
     */
    public Task<List<Transaction>> getTransactionsByCriteriaTask(User user, LocalDate from, LocalDate to, Category category) {
        return new Task<>() {
            @Override
            protected List<Transaction> call() throws Exception {
                return getTransactionsByCriteria(user, from, to, category);
            }
        };
    }

    public void addTransaction(User user, Category category, double amount, String type, String date) {
        validateTransactionData(user, category, amount, type, date);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type.trim());
        transaction.setDate(date.trim());

        transactionRepository.save(transaction);
    }

    public void updateTransaction(int id, User user, Category category, double amount, String type, String date) {
        validateTransactionData(user, category, amount, type, date);

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type.trim());
        transaction.setDate(date.trim());

        transactionRepository.save(transaction);
    }

    public void deleteTransaction(int id, User user) {
        transactionRepository.delete(id);
    }

    public double getTotalIncome() {
        return transactionRepository.findByType("income").stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getTotalExpenses() {
        return transactionRepository.findByType("expense").stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private void validateTransactionData(User user, Category category, double amount, String type, String date) {
        if (user == null || category == null || type == null || date == null) {
            throw new IllegalArgumentException("All transaction fields are required.");
        }
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value.");
        }

        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        if (!normalizedType.equals("income") && !normalizedType.equals("expense")) {
            throw new IllegalArgumentException("Transaction type must be either 'Income' or 'Expense'.");
        }

        try {
            LocalDate.parse(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD.");
        }

        if (category.getUser() == null || category.getUser().getId() != user.getId()) {
             throw new IllegalArgumentException("The selected category does not belong to your account.");
        }
    }
}
