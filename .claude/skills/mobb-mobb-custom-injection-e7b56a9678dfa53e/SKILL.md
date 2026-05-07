---
name: mobb-mobb-custom-injection-e7b56a9678dfa53e
description: Apply this skill when generating Java code that constructs SQL queries with user-supplied input to prevent SQL injection vulnerabilities.
---

## SQL Injection Prevention via Prepared Statements

SQL injection occurs when user-supplied input is concatenated directly into SQL query strings. An attacker can inject malicious SQL syntax to alter query logic, bypass authentication, or access unauthorized data.

### ❌ Unsafe Pattern: String Concatenation

Never concatenate user input directly into SQL strings:

```java
String username = getUserInput();
String password = getUserInput();
String sql = "SELECT * FROM users WHERE username = '" + username + 
            "' AND password = '" + password + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

This allows an attacker to inject SQL by providing input like `' OR '1'='1` to bypass authentication.

### ✅ Safe Pattern: Parameterized Queries with PreparedStatement

Always use `PreparedStatement` with parameter placeholders (`?`) to separate SQL structure from data:

```java
String username = getUserInput();
String password = getUserInput();
String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, username);
stmt.setString(2, password);
ResultSet rs = stmt.executeQuery();
```

### Key Requirements

1. **Use placeholders (`?`)** in the SQL template instead of concatenating values
2. **Use `PreparedStatement`** instead of `Statement`
3. **Call `conn.prepareStatement(sql)`** with the parameterized query
4. **Bind parameters** using type-specific setter methods:
   - `stmt.setString(index, value)` for text
   - `stmt.setInt(index, value)` for integers
   - `stmt.setDouble(index, value)` for decimals
   - `stmt.setLong(index, value)` for long integers
5. **Execute without passing the SQL string** to `executeQuery()` or `executeUpdate()`

### Pattern for LIKE Clauses

When using `LIKE` with wildcards, construct the pattern value before binding:

```java
String searchTerm = getUserInput();
String sql = "SELECT * FROM table WHERE column LIKE ?";

PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, "%" + searchTerm + "%");  // Wildcards added to the value, not the query
ResultSet rs = stmt.executeQuery();
```

### Pattern for INSERT/UPDATE Statements

The same principle applies to INSERT and UPDATE operations:

```java
String sql = "INSERT INTO table (col1, col2, col3) VALUES (?, ?, ?)";

PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, value1);
stmt.setString(2, value2);
stmt.setString(3, value3);
int result = stmt.executeUpdate();
```

### Type Conversion Handling

When input requires type conversion (e.g., string to integer), handle conversion errors gracefully:

```java
String userId = getUserInput();
String sql = "SELECT * FROM accounts WHERE user_id = ?";

PreparedStatement stmt = conn.prepareStatement(sql);
try {
    stmt.setInt(1, Integer.parseInt(userId));
} catch (NumberFormatException e) {
    // Log the error and use a safe default value
    stmt.setInt(1, 0);
}
ResultSet rs = stmt.executeQuery();
```

### Why This Works

`PreparedStatement` separates the SQL command structure from the data. The database driver parses the SQL template once and treats all bound parameters as literal data values, never as executable SQL code. This prevents injection regardless of the input content.
