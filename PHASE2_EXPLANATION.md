# 📚 PHASE 2 - Database Models & Entities (Complete Explanation)

## 🎯 Overview: What We Built

Successfully completed **Phase 2** - Created all JPA entities and repository interfaces for the Personal Finance Tracker API.

---
## 🎓 Key Learnings for Interviews

### **1. Entity Modeling**
- How to map Java classes to database tables
- Choosing appropriate data types
- Relationship cardinality (One-to-Many, Many-to-One)

### **2. JPA Best Practices**
- LAZY fetching by default
- Proper cascade configuration
- Index placement for performance
- Unique constraints for business rules

### **3. Spring Data JPA**
- Repository pattern benefits
- Query derivation magic
- Custom JPQL queries
- Method naming conventions

### **4. Database Design**
- Normalization
- Foreign key relationships
- Composite unique constraints
- Performance indexing

---

## 📦 What Was Created

### **Entities (4)**
1. ✅ **User** - User authentication and profile
2. ✅ **Category** - Transaction categories (Income/Expense)
3. ✅ **Transaction** - Financial transactions
4. ✅ **Budget** - Monthly spending limits

### **Enums (2)**
1. ✅ **TransactionType** - INCOME, EXPENSE
2. ✅ **PaymentMethod** - CASH, CREDIT_CARD, DEBIT_CARD, UPI, BANK_TRANSFER

### **Repositories (4)**
1. ✅ **UserRepository** - User CRUD + custom queries
2. ✅ **CategoryRepository** - Category CRUD + custom queries
3. ✅ **TransactionRepository** - Transaction CRUD + custom queries
4. ✅ **BudgetRepository** - Budget CRUD + custom queries

---

## 🗄️ Database Schema Design

### **Entity Relationship Diagram (ERD)**

```
User (1) ←→ (Many) Transaction
User (1) ←→ (Many) Category
User (1) ←→ (Many) Budget

Category (1) ←→ (Many) Transaction
Category (1) ←→ (Many) Budget
```

### **Tables Created by Hibernate:**

1. **users**
   - Primary Key: `id`
   - Unique: `email`
   - Fields: `email`, `password`, `first_name`, `last_name`, `created_at`, `updated_at`

2. **categories**
   - Primary Key: `id`
   - Foreign Key: `user_id` (nullable for default categories)
   - Fields: `name`, `type`, `is_default`, `created_at`

3. **transactions**
   - Primary Key: `id`
   - Foreign Keys: `user_id`, `category_id`
   - Fields: `amount`, `description`, `transaction_date`, `payment_method`, `transaction_type`, `created_at`, `updated_at`
   - Indexes: `idx_user_date`, `idx_category`

4. **budgets**
   - Primary Key: `id`
   - Foreign Keys: `user_id`, `category_id`
   - Fields: `limit_amount`, `month`, `year`, `created_at`
   - Unique Constraint: `user_id + category_id + month + year`
   - Index: `idx_user_month_year`

---

## 🎓 Interview Preparation: Key Concepts

### 1. **JPA Annotations Explained**

#### `@Entity`
```java
@Entity
@Table(name = "users")
public class User { ... }
```
**Interview Answer:** 
"@Entity marks a class as a JPA entity that will be mapped to a database table. @Table specifies the table name - without it, JPA uses the class name."

#### `@Id` and `@GeneratedValue`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
**Interview Answer:**
"@Id marks the primary key field. @GeneratedValue with IDENTITY strategy tells the database to auto-generate IDs using AUTO_INCREMENT (MySQL) or SERIAL (PostgreSQL)."

**Other strategies:**
- `IDENTITY` - Database auto-increment
- `SEQUENCE` - Database sequence (PostgreSQL preferred)
- `TABLE` - Separate table for ID generation
- `AUTO` - Let JPA choose based on database

#### `@Column`
```java
@Column(nullable = false, unique = true, length = 100)
private String email;
```
**Interview Answer:**
"@Column customizes the column mapping. We can specify nullable, unique, length, precision, scale, etc. Without @Column, JPA uses field name as column name."

#### `@Enumerated`
```java
@Enumerated(EnumType.STRING)
@Column(name = "transaction_type", length = 10)
private TransactionType transactionType;
```
**Interview Answer:**
"@Enumerated maps Java enums to database. EnumType.STRING stores the enum name (e.g., 'INCOME'), while EnumType.ORDINAL stores the position (0, 1, 2). STRING is preferred because it's more readable and migration-safe."

---

### 2. **JPA Relationships**

#### `@OneToMany`
```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Transaction> transactions = new ArrayList<>();
```

**Interview Answer:**
"One user has many transactions. `mappedBy='user'` means Transaction entity owns the relationship. `cascade=ALL` means operations on User cascade to Transactions. `orphanRemoval=true` means if we remove a transaction from the list, it's deleted from database."

#### `@ManyToOne`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

**Interview Answer:**
"Many transactions belong to one user. @JoinColumn creates the foreign key column. LAZY fetching means user data is only loaded when accessed, improving performance."

---

### 3. **Fetch Strategies: LAZY vs EAGER**

| Strategy | When Data Loads | Use Case |
|----------|----------------|----------|
| **LAZY** | Only when accessed | Large collections, optional data |
| **EAGER** | Immediately with parent | Small, always-needed data |

**Interview Answer:**
"I used LAZY fetching for all relationships because:
1. **Performance** - Avoids loading unnecessary data
2. **N+1 Prevention** - Prevents multiple queries
3. **Control** - Load data when needed using JOIN FETCH

EAGER would load all related data immediately, which can cause performance issues with large datasets."

**Example N+1 Problem:**
```java
// BAD: EAGER fetch causes N+1 queries
List<User> users = userRepository.findAll(); // 1 query
users.forEach(u -> u.getTransactions()); // N queries (one per user)

// GOOD: LAZY with JOIN FETCH
@Query("SELECT u FROM User u LEFT JOIN FETCH u.transactions")
List<User> findAllWithTransactions(); // 1 query total!
```

---

### 4. **Cascade Types**

| Cascade Type | What It Does |
|--------------|--------------|
| **ALL** | All operations cascade |
| **PERSIST** | Save cascades to children |
| **MERGE** | Update cascades to children |
| **REMOVE** | Delete cascades to children |
| **REFRESH** | Reload cascades to children |
| **DETACH** | Detach cascades to children |

**Interview Answer:**
"I used `CascadeType.ALL` on User relationships because when a user is deleted, all their transactions, categories, and budgets should be deleted too (data integrity). However, I didn't cascade from Transaction to Category because deleting a transaction shouldn't delete the category."

---

### 5. **Unique Constraints**

```java
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_category_month_year", 
                     columnNames = {"user_id", "category_id", "month", "year"})
})
```

**Interview Answer:**
"This composite unique constraint prevents duplicate budgets. A user can't have two budgets for the same category in the same month/year. The constraint is enforced at database level, which is faster and more reliable than application-level validation."

---

### 6. **Indexes for Performance**

```java
@Table(indexes = {
    @Index(name = "idx_user_date", columnList = "user_id, transaction_date"),
    @Index(name = "idx_category", columnList = "category_id")
})
```

**Interview Answer:**
"Indexes speed up queries. I added:
- `idx_user_date` - For queries like 'get user's transactions for a date range'
- `idx_category` - For category-based filtering

Composite index (user_id, transaction_date) is used when querying by user alone OR by both user and date. Order matters!"

**Index vs No Index:**
- Without index: Full table scan (slow for large tables)
- With index: B-tree lookup (logarithmic time)

---

### 7. **@CreationTimestamp & @UpdateTimestamp**

```java
@CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

**Interview Answer:**
"Hibernate automatically sets these fields:
- `@CreationTimestamp` - Set once when entity is created
- `@UpdateTimestamp` - Updated every time entity is modified

`updatable=false` on created_at ensures it never changes after creation."

---

## 🔍 Repository Pattern Explained

### **Spring Data JPA Magic**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**Interview Question: "How does Spring Data JPA generate queries from method names?"**

**Answer:**
"Spring Data JPA parses method names following a convention:
- `findBy` - SELECT query
- `Email` - WHERE email = ?
- Returns `Optional<User>` - null-safe return

Spring generates:
```sql
SELECT * FROM users WHERE email = ?
```

This is called **Query Derivation from Method Names**."

### **Common Query Keywords:**

| Method Prefix | SQL Equivalent |
|---------------|----------------|
| `findBy` | SELECT |
| `existsBy` | SELECT COUNT > 0 |
| `deleteBy` | DELETE |
| `countBy` | SELECT COUNT |
| `findByFieldOrderByAnotherField` | ORDER BY |
| `findByFieldBetween` | WHERE field BETWEEN ? AND ? |

---

### **Custom JPQL Queries**

```java
@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
       "WHERE t.user.id = :userId AND t.category.id = :categoryId " +
       "AND t.transactionDate BETWEEN :startDate AND :endDate")
BigDecimal sumAmountByUserAndCategoryAndDateRange(
    @Param("userId") Long userId,
    @Param("categoryId") Long categoryId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

**Interview Answer:**
"When method name queries become complex, we use @Query with JPQL (Java Persistence Query Language). 
- JPQL uses entity names, not table names
- `COALESCE(SUM(t.amount), 0)` - Returns 0 if no results (prevents null)
- `@Param` - Binds method parameters to query parameters
- This is more readable than a complex method name!"

---

## 🎯 Design Decisions & Rationale

### 1. **Why BigDecimal for Money?**

```java
@Column(precision = 10, scale = 2)
private BigDecimal amount;
```

**Interview Answer:**
"BigDecimal is used for financial calculations because:
- **Precision** - No floating-point errors (unlike double/float)
- **Scale** - Exact decimal places (e.g., 2 for currency)
- **Example:** 0.1 + 0.2 = 0.30000000000000004 (double) vs 0.30 (BigDecimal)

`precision=10, scale=2` means max 10 digits with 2 decimal places (99999999.99)"

### 2. **Why LocalDate vs LocalDateTime?**

```java
private LocalDate transactionDate; // For Transaction
private LocalDateTime createdAt;   // For audit
```

**Interview Answer:**
"LocalDate for transaction date because we care about the day, not the exact time. LocalDateTime for audit fields because we need exact timestamp. This also makes date-range queries simpler."

### 3. **Why orphanRemoval=true?**

```java
@OneToMany(mappedBy = "user", orphanRemoval = true)
private List<Transaction> transactions;
```

**Interview Answer:**
"orphanRemoval=true means if a transaction is removed from user's transaction list but not explicitly deleted, JPA will delete it from database. Without this, it would remain in database as an orphan record."

### 4. **Why FetchType.LAZY everywhere?**

**Interview Answer:**
"Default fetching is:
- @ManyToOne: EAGER (bad!)
- @OneToMany: LAZY (good)

I explicitly set LAZY for all @ManyToOne to avoid N+1 problems. I'll use JOIN FETCH in queries when I need related data."

---

## 🚀 What Happens When Application Starts?

### **Hibernate DDL Auto = update**

When you run the application with `spring.jpa.hibernate.ddl-auto=update`:

1. **Hibernate scans entities** - Finds @Entity classes
2. **Generates DDL** - Creates CREATE TABLE statements
3. **Compares with database** - Checks existing schema
4. **Updates schema** - Adds missing tables/columns

**Generated SQL (MySQL):**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL,
    is_default BOOLEAN NOT NULL,
    user_id BIGINT,
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    transaction_date DATE NOT NULL,
    payment_method VARCHAR(20),
    transaction_type VARCHAR(10) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_user_date ON transactions(user_id, transaction_date);
CREATE INDEX idx_category ON transactions(category_id);

CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    limit_amount DECIMAL(10,2) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    UNIQUE KEY uk_user_category_month_year (user_id, category_id, month, year)
);

CREATE INDEX idx_user_month_year ON budgets(user_id, month, year);
```

---

## 🎓 Common Interview Questions

### Q1: "Explain the difference between CascadeType.ALL and orphanRemoval=true"

**Answer:**
"CascadeType propagates operations (save, delete, update) from parent to child. orphanRemoval deletes child when removed from parent's collection.

Example:
```java
user.getTransactions().remove(transaction); // orphanRemoval deletes it
entityManager.remove(user); // CascadeType.REMOVE deletes transactions
```"

### Q2: "What's the N+1 problem and how do you solve it?"

**Answer:**
"N+1 happens when you load N entities, then make N additional queries for related data.

**Problem:**
```java
List<User> users = userRepo.findAll(); // 1 query
users.forEach(u -> u.getTransactions().size()); // N queries!
```

**Solutions:**
1. JOIN FETCH in JPQL
2. @EntityGraph
3. Batch fetching
4. DTOs with custom queries

**Best Solution:**
```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.transactions")
List<User> findAllWithTransactions();
```"

### Q3: "Why use `@Data` from Lombok?"

**Answer:**
"@Data generates:
- Getters for all fields
- Setters for non-final fields
- toString() method
- equals() and hashCode()
- Constructor for required fields

This reduces boilerplate by ~200 lines per entity! But be careful with bidirectional relationships in toString() - it can cause infinite recursion."

### Q4: "Explain hibernate.ddl-auto options"

**Answer:**
| Option | What It Does | When to Use |
|--------|--------------|-------------|
| `none` | No schema changes | Production |
| `validate` | Only validates schema | Production |
| `update` | Updates schema | Development |
| `create` | Drops and creates | Never in production! |
| `create-drop` | Creates, drops on shutdown | Testing |

**Production:** Use Flyway/Liquibase for migrations, set ddl-auto to `validate`"

---

## 📊 Phase 2 Statistics

| Metric | Count |
|--------|-------|
| **Entities Created** | 4 (User, Category, Transaction, Budget) |
| **Enums Created** | 2 (TransactionType, PaymentMethod) |
| **Repositories Created** | 4 |
| **Total Java Files** | 10 |
| **Custom Query Methods** | 15+ |
| **Relationships Defined** | 6 (@OneToMany, @ManyToOne) |
| **Indexes Added** | 3 |
| **Unique Constraints** | 2 |

---

## ✅ Verification Checklist

After Phase 2:
- [x] All entities compile successfully
- [x] Relationships properly mapped
- [x] Repositories extend JpaRepository
- [x] Custom query methods defined
- [x] Indexes added for performance
- [x] Unique constraints for data integrity
- [x] Timestamps configured
- [x] Build successful (mvn compile)

---

## 🚀 Next Steps: Phase 3

In Phase 3, we'll implement:
1. **JWT utility class** - Token generation and validation
2. **Authentication filter** - Intercept requests and validate JWT
3. **Security configuration** - Protect endpoints
4. **Auth DTOs** - LoginRequest, RegisterRequest, AuthResponse
5. **Auth service** - Registration and login logic
6. **Auth controller** - REST endpoints for auth

---

## 💡 Pro Tips for Interviews

1. **Always explain trade-offs**: "I used LAZY fetching because... but EAGER would be better if..."
2. **Mention alternatives**: "I used @Query, but Criteria API would work for dynamic queries"
3. **Show production awareness**: "In development I use ddl-auto=update, but in production I'd use Flyway"
4. **Discuss performance**: "I added indexes on frequently queried columns to improve performance"
5. **Be specific**: Don't just say "JPA handles it" - explain HOW

---


---

*Created: September 30, 2025*  
*Phase: 2 of 7 - COMPLETE ✅*  
*Next: Phase 3 - Security & JWT Authentication*
