package com.demobank.service;

import com.demobank.entity.User;
import com.demobank.entity.Account;
import com.demobank.entity.CreditApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import com.demobank.entity.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Service
public class BankService {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * User authentication method
     */
    public User authenticateUser(String username, String password) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

            System.out.println("Executing SQL: " + sql);

            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get user accounts
     */
    public List<Account> getUserAccounts(String userId) {
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM accounts WHERE user_id = ?";

            System.out.println("Executing SQL: " + sql);

            PreparedStatement stmt = conn.prepareStatement(sql);
            try {
                long uid = Long.parseLong(userId);
                stmt.setLong(1, uid);
            } catch (NumberFormatException e) {
                // If the userId is not a valid number, set to 0 to avoid SQL errors
                stmt.setLong(1, 0L);
            }
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Account account = new Account();
                account.setId(rs.getLong("id"));
                account.setUserId(rs.getLong("user_id"));
                account.setAccountType(rs.getString("account_type"));
                account.setBalance(rs.getBigDecimal("balance"));
                account.setAccountNumber(rs.getString("account_number"));
                accounts.add(account);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }
    
    /**
     * Money transfer method
     */
    public boolean transferMoney(String fromAccountId, String toAccountId, String amount) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";

            System.out.println("Executing SQL: " + debitSql);
            System.out.println("Executing SQL: " + creditSql);

            try (PreparedStatement psDebit = conn.prepareStatement(debitSql)) {
                psDebit.setBigDecimal(1, new BigDecimal(amount));
                psDebit.setLong(2, Long.parseLong(fromAccountId));
                psDebit.executeUpdate();
            }

            try (PreparedStatement psCredit = conn.prepareStatement(creditSql)) {
                psCredit.setBigDecimal(1, new BigDecimal(amount));
                psCredit.setLong(2, Long.parseLong(toAccountId));
                psCredit.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Credit application search
     */
    public List<CreditApplication> searchCreditApplications(String searchTerm) {
        List<CreditApplication> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM credit_applications WHERE employment_status LIKE ? OR comments LIKE ?";

            System.out.println("Executing SQL: " + sql);

            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                CreditApplication app = new CreditApplication();
                app.setId(rs.getLong("id"));
                app.setUserId(rs.getLong("user_id"));
                app.setRequestedLimit(rs.getBigDecimal("requested_limit"));
                app.setAnnualIncome(rs.getBigDecimal("annual_income"));
                app.setEmploymentStatus(rs.getString("employment_status"));
                app.setStatus(rs.getString("status"));
                app.setComments(rs.getString("comments"));
                applications.add(app);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return applications;
    }
    
    /**
     * Create a new credit application
     */
    public boolean createCreditApplication(String userId, String requestedLimit, 
                                         String annualIncome, String employmentStatus, String comments) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO credit_applications (user_id, requested_limit, annual_income, " +
                        "employment_status, status, application_date, comments) VALUES (?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, ?)";

            System.out.println("Executing SQL: " + sql);

            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            try {
                stmt.setLong(1, Long.parseLong(userId));
                stmt.setBigDecimal(2, new BigDecimal(requestedLimit));
                stmt.setBigDecimal(3, new BigDecimal(annualIncome));
            } catch (NumberFormatException nfe) {
                // Failed to parse numeric values -> reject
                return false;
            }
            stmt.setString(4, employmentStatus);
            stmt.setString(5, comments);
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get account by account number
     */
    public Account getAccountByNumber(String accountNumber) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM accounts WHERE account_number = ?";

            System.out.println("Executing SQL: " + sql);

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Account account = new Account();
                account.setId(rs.getLong("id"));
                account.setUserId(rs.getLong("user_id"));
                account.setAccountType(rs.getString("account_type"));
                account.setBalance(rs.getBigDecimal("balance"));
                account.setAccountNumber(rs.getString("account_number"));
                return account;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search transactions for a user's accounts with various filters.
     * Uses PreparedStatement to avoid SQL injection.
     */
    public List<Transaction> searchTransactions(String userId,
                                                String minAmount,
                                                String maxAmount,
                                                String startDate,
                                                String endDate,
                                                String description,
                                                String[] types) {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.id WHERE a.user_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(Long.parseLong(userId));

            if (minAmount != null && !minAmount.isBlank()) {
                sql.append(" AND t.amount >= ?");
                params.add(new BigDecimal(minAmount));
            }
            if (maxAmount != null && !maxAmount.isBlank()) {
                sql.append(" AND t.amount <= ?");
                params.add(new BigDecimal(maxAmount));
            }
            if (startDate != null && !startDate.isBlank()) {
                sql.append(" AND t.transaction_date >= ?");
                LocalDate ld = LocalDate.parse(startDate);
                params.add(Timestamp.valueOf(ld.atStartOfDay()));
            }
            if (endDate != null && !endDate.isBlank()) {
                sql.append(" AND t.transaction_date <= ?");
                LocalDate ld = LocalDate.parse(endDate);
                params.add(Timestamp.valueOf(ld.atTime(23, 59, 59)));
            }
            if (description != null && !description.isBlank()) {
                sql.append(" AND LOWER(t.description) LIKE ?");
                params.add("%" + description.toLowerCase() + "%");
            }
            if (types != null && types.length > 0) {
                sql.append(" AND t.type IN (");
                StringJoiner sj = new StringJoiner(",");
                for (int i = 0; i < types.length; i++) {
                    sj.add("?");
                    params.add(types[i]);
                }
                sql.append(sj.toString()).append(")");
            }

            sql.append(" ORDER BY t.transaction_date DESC");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) {
                    ps.setLong(i + 1, (Long) p);
                } else if (p instanceof BigDecimal) {
                    ps.setBigDecimal(i + 1, (BigDecimal) p);
                } else if (p instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) p);
                } else {
                    ps.setString(i + 1, p.toString());
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setAccountId(rs.getLong("account_id"));
                t.setType(rs.getString("type"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setDescription(rs.getString("description"));
                Timestamp ts = rs.getTimestamp("transaction_date");
                if (ts != null) {
                    t.setTransactionDate(ts.toLocalDateTime());
                }
                transactions.add(t);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return transactions;
    }
}