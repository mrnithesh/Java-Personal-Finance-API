# 📚 PHASE 5 - Budget Management & Spending Tracking

## Complete Explanation for Learning & Interviews

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Components Deep Dive](#components-deep-dive)
4. [Calculation Algorithms](#calculation-algorithms)
5. [Design Decisions](#design-decisions)
6. [Interview Questions & Answers](#interview-questions--answers)
7. [Performance Optimization](#performance-optimization)
8. [Best Practices](#best-practices)

---

## Overview

### What We Built

Phase 5 implements **budget tracking with real-time spending calculation** and **intelligent alerts**:
- Monthly budgets per category
- Automatic spending calculation from transactions
- Alert system (80% and 100% thresholds)
- Duplicate prevention
- Complete CRUD operations

### Why Budget Tracking?

| Without Budgets | With Budgets |
|-----------------|--------------|
| Reactive spending | Proactive planning |
| No spending limits | Clear boundaries |
| Surprises at month-end | Early warnings |
| Hard to control expenses | Guided spending |

---

## Core Concepts

### 1. Budget Entity

```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_category_month_year",
        columnNames = {"user_id", "category_id", "month", "year"})
})
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal limitAmount;
    
    @Column(nullable = false)
    private Integer month;  // 1-12
    
    @Column(nullable = false)
    private Integer year;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**Key Design Decisions:**

#### Composite Unique Constraint
```sql
UNIQUE (user_id, category_id, month, year)
```

**Why?**
- One budget per category per month per user
- Prevents confusion (multiple budgets for same thing)
- Clear business rule enforcement

**Example:**
```
✅ User1, Food, October 2025 → OK
✅ User1, Food, November 2025 → OK (different month)
✅ User2, Food, October 2025 → OK (different user)
❌ User1, Food, October 2025 → DUPLICATE (already exists)
```

#### Separate Month and Year Columns

**Why not store as Date?**

```java
// BAD: Use date for month
private LocalDate month;  // 2025-10-01

// GOOD: Separate month and year
private Integer month;  // 10
private Integer year;   // 2025
```

**Reasons:**
1. **Semantics:** Budget is for entire month, not specific date
2. **Queries:** Easier to filter by month/year
3. **Indexing:** Better index performance
4. **Clarity:** Clear business meaning

---

### 2. Spending Calculation Pattern

**Two Approaches:**

#### Approach 1: Calculate On-Demand (Current)

```java
// Calculate when needed
public BigDecimal getCurrentSpending(Budget budget) {
    LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
    LocalDate end = start.plusMonths(1).minusDays(1);
    
    List<Transaction> transactions = repository.findByUserAndDateRange(
        budget.getUser().getId(), start, end
    );
    
    return transactions.stream()
        .filter(t -> t.getCategory().equals(budget.getCategory()))
        .filter(t -> t.getType() == TransactionType.EXPENSE)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**Pros:**
- ✅ Always accurate (real-time)
- ✅ No extra storage needed
- ✅ No synchronization issues
- ✅ Simple to implement

**Cons:**
- ❌ Calculated every time
- ❌ Performance impact with many transactions
- ❌ Repeated queries

#### Approach 2: Store and Update (Alternative)

```java
@Entity
public class Budget {
    private BigDecimal limitAmount;
    private BigDecimal currentSpending;  // Stored value
    
    public BigDecimal getRemaining() {
        return limitAmount.subtract(currentSpending);
    }
}

// Update on transaction changes
@TransactionalEventListener
public void onTransactionCreated(TransactionCreatedEvent event) {
    Budget budget = findBudgetForTransaction(event.getTransaction());
    if (budget != null) {
        budget.setCurrentSpending(
            budget.getCurrentSpending().add(event.getTransaction().getAmount())
        );
        budgetRepository.save(budget);
    }
}
```

**Pros:**
- ✅ Fast reads (no calculation needed)
- ✅ No query needed
- ✅ Scales better

**Cons:**
- ❌ Synchronization complexity
- ❌ Risk of data inconsistency
- ❌ Extra storage
- ❌ Need to handle transaction updates/deletes

**Our Choice: Approach 1**

**Why?**
- Simplicity over premature optimization
- Data consistency guaranteed
- Acceptable performance for typical usage
- Can optimize later if needed

---

### 3. Alert System

**Design Pattern: Threshold-Based Alerts**

```java
public List<BudgetAlertResponse> getBudgetAlerts(User currentUser) {
    // Get current month budgets
    List<Budget> budgets = repository.findByUserIdAndCurrentMonth(...);
    
    List<BudgetAlertResponse> alerts = new ArrayList<>();
    
    for (Budget budget : budgets) {
        BigDecimal spending = calculateCurrentSpending(budget);
        double percentage = calculatePercentage(spending, budget.getLimitAmount());
        
        // Alert if 80% or more used
        if (percentage >= 80.0) {
            String level = percentage >= 100.0 ? "DANGER" : "WARNING";
            String message = generateMessage(percentage, daysLeft);
            
            alerts.add(new BudgetAlertResponse(...));
        }
    }
    
    return alerts;
}
```

**Alert Thresholds:**

```
  0% ─────────────── 79%: ✅ OK (Green)
 80% ────────────── 99%: ⚠️ WARNING (Yellow)
100% ─────────────────: 🚨 DANGER (Red)
```

**Why These Thresholds?**

**80% (WARNING):**
- Early enough to adjust spending
- Not too early to cause alert fatigue
- Industry standard

**100% (DANGER):**
- Budget exceeded
- Immediate attention needed
- Different tone/urgency

---

## Components Deep Dive

### 1. BudgetService

#### Create Budget Flow

```java
@Transactional
public BudgetResponse createBudget(BudgetRequest request, User currentUser) {
    
    // Step 1: Validate category exists
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException(...));
    
    // Step 2: Check user can access category
    if (category.getUser() != null && 
        !category.getUser().getId().equals(currentUser.getId())) {
        throw new UnauthorizedAccessException(...);
    }
    
    // Step 3: Check for duplicate
    Optional<Budget> existing = budgetRepository
        .findByUserIdAndCategoryIdAndMonthAndYear(
            currentUser.getId(), 
            request.getCategoryId(), 
            request.getMonth(), 
            request.getYear()
        );
    
    if (existing.isPresent()) {
        throw new DuplicateResourceException(...);
    }
    
    // Step 4: Create budget
    Budget budget = new Budget();
    budget.setUser(currentUser);
    budget.setCategory(category);
    budget.setLimitAmount(request.getLimitAmount());
    budget.setMonth(request.getMonth());
    budget.setYear(request.getYear());
    
    // Step 5: Save and return
    Budget saved = budgetRepository.save(budget);
    return mapToResponse(saved);  // Includes spending calculation
}
```

**Interview Question:** *"Why check for duplicates in service layer when there's a database constraint?"*

**Answer:**
"Defense in depth approach:

**Database Constraint:**
```sql
UNIQUE (user_id, category_id, month, year)
```
- **Last line of defense**
- Throws generic `DataIntegrityViolationException`
- Returns 500 Internal Server Error (not ideal)

**Service Layer Check:**
```java
if (existing.isPresent()) {
    throw new DuplicateResourceException("Budget already exists...");
}
```
- **First line of defense**
- Throws business exception
- Returns 409 Conflict with clear message
- Better user experience

**Both are needed:**
- Service check: Better UX
- Database constraint: Data integrity guarantee"

#### Update Budget - Handling Duplicates

```java
public BudgetResponse updateBudget(Long id, BudgetRequest request, User user) {
    Budget budget = findAndVerifyOwnership(id, user);
    
    // Check if category/month/year changed
    boolean keysChanged = 
        !budget.getCategory().getId().equals(request.getCategoryId()) ||
        !budget.getMonth().equals(request.getMonth()) ||
        !budget.getYear().equals(request.getYear());
    
    if (keysChanged) {
        // Check if new combination already exists
        Optional<Budget> existing = budgetRepository
            .findByUserIdAndCategoryIdAndMonthAndYear(...);
        
        // Make sure it's not the same budget we're updating
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new DuplicateResourceException(...);
        }
    }
    
    // Proceed with update
    budget.setCategory(...);
    budget.setLimitAmount(...);
    // ...
}
```

**Why This Logic?**

**Scenario 1: Only amount changed**
```java
Budget ID 1: {category: Food, month: 10, year: 2025, limit: 3000}
Update to: {category: Food, month: 10, year: 2025, limit: 5000}
Result: ✅ OK (keys same, no duplicate check needed)
```

**Scenario 2: Category changed**
```java
Budget ID 1: {category: Food, month: 10, year: 2025}
Update to: {category: Transport, month: 10, year: 2025}

Check: Does user have budget for Transport in Oct 2025?
- No: ✅ OK
- Yes (ID 2): ❌ Duplicate
- Yes (ID 1): ✅ OK (same budget)
```

---

### 2. Spending Calculation Deep Dive

```java
private BigDecimal calculateCurrentSpending(Budget budget) {
    
    // 1. Calculate month boundaries
    LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
    LocalDate endDate = startDate.plusMonths(1).minusDays(1);
    
    // Example: October 2025
    // startDate: 2025-10-01
    // endDate: 2025-10-31
    
    // 2. Get all user transactions in date range
    List<Transaction> transactions = transactionRepository
        .findByUserIdAndTransactionDateBetween(
            budget.getUser().getId(), 
            startDate, 
            endDate
        );
    
    // 3. Filter and sum
    BigDecimal total = transactions.stream()
        .filter(t -> t.getCategory().getId().equals(budget.getCategory().getId()))
        .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    return total;
}
```

**Step-by-Step Example:**

```
Budget: Food & Dining, October 2025, Limit: 3000

Transactions in October 2025:
1. Oct 5:  1500 (Food & Dining, EXPENSE) → Include ✅
2. Oct 10: 800  (Food & Dining, EXPENSE) → Include ✅
3. Oct 15: 5000 (Salary, INCOME) → Exclude (INCOME)
4. Oct 20: 2000 (Transport, EXPENSE) → Exclude (different category)
5. Nov 1:  500  (Food & Dining, EXPENSE) → Exclude (different month)

Total: 1500 + 800 = 2300
Percentage: 2300 / 3000 = 76.67%
Status: OK (< 80%)
```

#### Month Boundary Handling

```java
// Handle all edge cases
YearMonth yearMonth = YearMonth.of(year, month);
LocalDate start = yearMonth.atDay(1);         // First day
LocalDate end = yearMonth.atEndOfMonth();     // Last day (handles Feb!)

// Examples:
YearMonth.of(2024, 2).atEndOfMonth()  // 2024-02-29 (leap year)
YearMonth.of(2025, 2).atEndOfMonth()  // 2025-02-28 (regular year)
YearMonth.of(2025, 4).atEndOfMonth()  // 2025-04-30 (30-day month)
YearMonth.of(2025, 10).atEndOfMonth() // 2025-10-31 (31-day month)
```

---

### 3. Percentage Calculation with BigDecimal

**The Problem with Double:**

```java
// WRONG: Using double
double spending = 2300.50;
double limit = 3000.00;
double percentage = (spending / limit) * 100;
// Result: 76.68333333333334 (many decimal places, rounding issues)
```

**The BigDecimal Solution:**

```java
private double calculatePercentage(BigDecimal spending, BigDecimal limit) {
    
    // Check for zero (prevent division by zero)
    if (limit.compareTo(BigDecimal.ZERO) == 0) {
        return 0.0;
    }
    
    // Divide with scale and rounding mode
    BigDecimal result = spending
        .divide(limit, 4, RoundingMode.HALF_UP)  // 4 decimal places
        .multiply(BigDecimal.valueOf(100));       // Convert to percentage
    
    return result.doubleValue();  // Convert to double for response
}
```

**Step-by-Step:**

```
Spending: 2300.00
Limit: 3000.00

Step 1: Divide
2300.00 / 3000.00 = 0.7666...

Step 2: Set scale (4 decimal places, HALF_UP rounding)
0.7666... → 0.7667

Step 3: Multiply by 100
0.7667 × 100 = 76.67

Step 4: Convert to double
76.67 (double)
```

**Why Scale 4?**
- Accurate for percentages (76.6667%)
- Not too precise (avoiding false precision)
- Standard for financial calculations

**Rounding Modes:**

```java
BigDecimal value = new BigDecimal("0.765");

RoundingMode.UP          → 0.77  (always round up)
RoundingMode.DOWN        → 0.76  (always round down)
RoundingMode.CEILING     → 0.77  (toward positive infinity)
RoundingMode.FLOOR       → 0.76  (toward negative infinity)
RoundingMode.HALF_UP     → 0.77  (standard rounding, >= 0.5 up)
RoundingMode.HALF_DOWN   → 0.76  (< 0.5 down)
RoundingMode.HALF_EVEN   → 0.76  (banker's rounding)
```

**Financial Standard:** `HALF_UP` (standard rounding)

---

### 4. Alert Generation

```java
public List<BudgetAlertResponse> getBudgetAlerts(User currentUser) {
    
    // 1. Get current date
    LocalDate now = LocalDate.now();
    int currentMonth = now.getMonthValue();
    int currentYear = now.getYear();
    
    // 2. Get all budgets for current month
    List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
        currentUser.getId(), currentMonth, currentYear
    );
    
    List<BudgetAlertResponse> alerts = new ArrayList<>();
    
    // 3. Process each budget
    for (Budget budget : budgets) {
        
        // Calculate spending and percentage
        BigDecimal spending = calculateCurrentSpending(budget);
        double percentage = calculatePercentage(spending, budget.getLimitAmount());
        
        // Only alert if 80% or more
        if (percentage >= 80.0) {
            
            // Calculate days left in month
            int daysLeft = YearMonth.of(currentYear, currentMonth)
                .lengthOfMonth() - now.getDayOfMonth();
            
            // Determine alert level
            String alertLevel = percentage >= 100.0 ? "DANGER" : "WARNING";
            
            // Generate contextual message
            String message = percentage >= 100.0
                ? String.format("Budget exceeded by %.2f%%", percentage - 100)
                : String.format("%.0f%% of budget used with %d days remaining", 
                    percentage, daysLeft);
            
            // Add to alerts
            alerts.add(BudgetAlertResponse.builder()
                .budgetId(budget.getId())
                .categoryName(budget.getCategory().getName())
                .limitAmount(budget.getLimitAmount())
                .currentSpending(spending)
                .percentageUsed(percentage)
                .daysLeftInMonth(daysLeft)
                .alertLevel(alertLevel)
                .message(message)
                .build());
        }
    }
    
    return alerts;
}
```

**Example Scenarios:**

```
Scenario 1: WARNING
- Date: October 5 (26 days left)
- Spending: 2400 / 3000 = 80%
- Message: "80% of budget used with 26 days remaining"
- Action: User can adjust spending

Scenario 2: WARNING (Critical)
- Date: October 28 (3 days left)
- Spending: 2700 / 3000 = 90%
- Message: "90% of budget used with 3 days remaining"
- Action: Urgent - minimal spending only

Scenario 3: DANGER
- Spending: 3150 / 3000 = 105%
- Message: "Budget exceeded by 5.00%"
- Action: Budget already exceeded

Scenario 4: DANGER (Exactly at limit)
- Spending: 3000 / 3000 = 100%
- Message: "Budget exceeded by 0.00%"
- Action: Stop spending in this category
```

---

## Design Decisions

### 1. Why Not Store Spending in Budget Table?

**Option A: Calculate On-Demand (Current)**
```java
@Entity
public class Budget {
    private BigDecimal limitAmount;
    // No currentSpending field
}

// Calculate when needed
BigDecimal spending = calculateCurrentSpending(budget);
```

**Option B: Store Spending (Alternative)**
```java
@Entity
public class Budget {
    private BigDecimal limitAmount;
    private BigDecimal currentSpending;  // Stored
}

// Update on every transaction change
```

**Why We Chose Option A:**

| Aspect | Calculate | Store |
|--------|-----------|-------|
| **Accuracy** | ✅ Always correct | ⚠️ Can get stale |
| **Consistency** | ✅ Guaranteed | ⚠️ Sync issues |
| **Complexity** | ✅ Simple | ❌ Complex |
| **Performance** | ⚠️ Slower reads | ✅ Fast reads |
| **Storage** | ✅ No extra column | ❌ Extra column |
| **Edge Cases** | ✅ Few | ❌ Many |

**When to Switch to Option B:**
- More than 1000 transactions per category per month
- Alert checks very frequent (every minute)
- Read performance critical
- Have robust event handling

---

### 2. Why Separate month and year Columns?

**Option A: Separate Columns (Current)**
```java
private Integer month;  // 1-12
private Integer year;   // 2025
```

**Option B: Single Date Column**
```java
private LocalDate monthDate;  // 2025-10-01
```

**Why Separate Columns?**

**Queries:**
```sql
-- Separate columns (clear intent)
WHERE month = 10 AND year = 2025

-- Single date (less clear)
WHERE MONTH(month_date) = 10 AND YEAR(month_date) = 2025
```

**Indexing:**
```sql
-- Separate: Composite index
INDEX idx_user_month_year (user_id, month, year)

-- Date: Function-based index needed (database-specific)
INDEX idx_user_month_date ON budgets(YEAR(month_date), MONTH(month_date))
```

**Semantics:**
- Budget is for ENTIRE month (not specific date)
- Month/year are discrete values (10, 2025)
- Date implies specific day (confusing)

---

### 3. Alert Threshold Selection

**Why 80% and 100%?**

**Research-Based:**
- 80/20 rule (Pareto principle)
- Psychological trigger point
- Time to course-correct

**Testing:**
```
50%: Too early (alert fatigue)
70%: Still time, but earlier warning good for some
80%: Sweet spot (urgent but actionable)
90%: Often too late to adjust
100%: Must know immediately
```

**Industry Standards:**
- Credit card alerts: 75-80%
- AWS billing alerts: 80%, 100%
- Azure cost management: 80%, 90%, 100%

**Customizable (Future Enhancement):**
```java
@Entity
public class Budget {
    private Integer warningThreshold = 80;  // Default 80%
    private Integer dangerThreshold = 100;  // Default 100%
}
```

---

## Interview Questions & Answers

### Beginner Level

**Q1: What is a budget in this system?**

**A:** A budget is a spending limit set for a specific category in a specific month. For example:
- Category: Food & Dining
- Month: October 2025
- Limit: $3000

The system tracks how much you've spent in that category that month and alerts you when approaching the limit.

**Q2: How does the system know how much I've spent?**

**A:** It calculates spending by:
1. Finding all your EXPENSE transactions
2. For the budget's category (e.g., Food)
3. In the budget's month (e.g., October 2025)
4. Summing the amounts

Example:
```
Budget: Food, October 2025, $3000

October Transactions:
- Oct 5: Groceries $500 (Food)
- Oct 10: Restaurant $200 (Food)
- Oct 15: Gas $50 (Transport) ← Not counted (different category)

Total Spent: $500 + $200 = $700
Percentage: 700/3000 = 23.33%
```

**Q3: Can I have multiple budgets for the same category?**

**A:** Yes, but not for the same month:
```
✅ Food, October 2025, $3000
✅ Food, November 2025, $2500
❌ Food, October 2025, $2000 ← Duplicate!
```

One budget per category per month keeps it simple.

### Intermediate Level

**Q4: Explain the spending calculation algorithm**

**A:**
```java
Step 1: Determine month boundaries
  - Start: First day of month (Oct 1, 2025)
  - End: Last day of month (Oct 31, 2025)

Step 2: Query transactions
  - User's transactions
  - Within date range
  - All categories (filter later)

Step 3: Filter transactions
  - Same category as budget
  - Type = EXPENSE (not INCOME)

Step 4: Sum amounts
  - Use BigDecimal for precision
  - Add all filtered amounts

Step 5: Return total
```

**Why filter in memory (Step 3) instead of in query?**
- Simpler repository methods
- Reuse existing queries
- Acceptable performance for typical data volumes
- Can optimize later if needed

**Q5: How do you prevent duplicate budgets?**

**A:** Two layers:

**Layer 1: Application Logic**
```java
Optional<Budget> existing = repository.findByUserCategoryMonthYear(...);
if (existing.isPresent()) {
    throw new DuplicateResourceException("Budget already exists");
}
```
- Better error message
- Returns 409 Conflict
- Good UX

**Layer 2: Database Constraint**
```sql
UNIQUE (user_id, category_id, month, year)
```
- Data integrity guarantee
- Even if app check fails
- Prevents race conditions

**Q6: Why use BigDecimal instead of double?**

**A:** Precision for money:
```java
// WRONG: double
double total = 0.0;
total += 0.1;
total += 0.2;
System.out.println(total);  // 0.30000000000000004 (!!)

// CORRECT: BigDecimal
BigDecimal total = BigDecimal.ZERO;
total = total.add(new BigDecimal("0.1"));
total = total.add(new BigDecimal("0.2"));
System.out.println(total);  // 0.3 (correct!)
```

**Why it matters:**
- Financial calculations must be exact
- Rounding errors accumulate
- Legal/regulatory requirements
- Customer trust

### Advanced Level

**Q7: How would you optimize spending calculation for large datasets?**

**A:** Several strategies:

**1. Caching (Redis)**
```java
@Cacheable(value = "budgetSpending", key = "#budget.id")
public BigDecimal calculateCurrentSpending(Budget budget) {
    // Calculated once, cached
}

@CacheEvict(value = "budgetSpending", allEntries = true)
public Transaction createTransaction(Transaction tx) {
    // Invalidate cache on transaction changes
}
```

**2. Materialized View**
```sql
CREATE MATERIALIZED VIEW budget_spending AS
SELECT 
    b.id as budget_id,
    COALESCE(SUM(t.amount), 0) as current_spending
FROM budgets b
LEFT JOIN transactions t ON 
    t.user_id = b.user_id
    AND t.category_id = b.category_id
    AND t.transaction_date BETWEEN ... AND ...
    AND t.transaction_type = 'EXPENSE'
GROUP BY b.id;

REFRESH MATERIALIZED VIEW budget_spending;  -- Refresh periodically
```

**3. Event Sourcing**
```java
@Entity
public class Budget {
    private BigDecimal currentSpending;
}

@EventListener
public void onTransactionCreated(TransactionCreatedEvent event) {
    Budget budget = findBudgetForTransaction(event.getTransaction());
    budget.addSpending(event.getTransaction().getAmount());
}

@EventListener
public void onTransactionDeleted(TransactionDeletedEvent event) {
    Budget budget = findBudgetForTransaction(event.getTransaction());
    budget.subtractSpending(event.getTransaction().getAmount());
}
```

**4. Database Aggregation**
```java
@Query("SELECT NEW BudgetSummary(b.id, b.limitAmount, " +
       "COALESCE(SUM(t.amount), 0)) " +
       "FROM Budget b " +
       "LEFT JOIN Transaction t ON " +
       "  t.user = b.user AND " +
       "  t.category = b.category AND " +
       "  t.transactionDate BETWEEN :start AND :end AND " +
       "  t.type = 'EXPENSE' " +
       "WHERE b.user.id = :userId " +
       "GROUP BY b.id, b.limitAmount")
List<BudgetSummary> findBudgetsWithSpending(@Param("userId") Long userId, ...);
```

**Q8: How to handle concurrent transaction creation?**

**A:** Race condition scenario:

```
Time  User Action            Database
T1    Create transaction     spending = 0
T2    Calculate spending     Read: 0
T3    Create transaction     spending = 0
T4    Calculate spending     Read: 100 (only one transaction!)
      Display: $100          Should be: $200
```

**Solutions:**

**1. Accept Eventually Consistent (Current)**
- Next calculation will be correct
- Acceptable for non-critical data
- Simple implementation

**2. Optimistic Locking**
```java
@Entity
public class Budget {
    @Version
    private Long version;
    private BigDecimal currentSpending;
}
```

**3. Pessimistic Locking**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Budget b WHERE b.id = :id")
Budget findByIdWithLock(@Param("id") Long id);
```

**4. Database Transaction Isolation**
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public BudgetResponse getBudget(Long id) {
    // Highest isolation level
}
```

**Q9: Design a notification system for budget alerts**

**A:** Multiple approaches:

**1. Scheduled Job (Batch)**
```java
@Scheduled(cron = "0 0 8 * * *")  // Daily at 8 AM
public void sendBudgetAlerts() {
    List<User> users = userRepository.findAll();
    
    for (User user : users) {
        List<BudgetAlert> alerts = budgetService.getAlertsForUser(user);
        
        if (!alerts.isEmpty()) {
            emailService.sendAlertEmail(user.getEmail(), alerts);
            
            // Update last notified time
            for (BudgetAlert alert : alerts) {
                alertRepository.saveLastNotification(alert, LocalDateTime.now());
            }
        }
    }
}
```

**2. Real-Time (Event-Driven)**
```java
@EventListener
public void onTransactionCreated(TransactionCreatedEvent event) {
    Transaction tx = event.getTransaction();
    
    // Find budget for this transaction
    Budget budget = budgetRepository.findByUserCategoryMonthYear(
        tx.getUser().getId(),
        tx.getCategory().getId(),
        tx.getTransactionDate().getMonthValue(),
        tx.getTransactionDate().getYear()
    ).orElse(null);
    
    if (budget != null) {
        BigDecimal spending = calculateCurrentSpending(budget);
        double percentage = calculatePercentage(spending, budget.getLimitAmount());
        
        // Check if alert threshold crossed
        if (percentage >= 80.0 && !wasAlertSentToday(budget)) {
            alertService.sendAlert(budget, percentage);
            markAlertSent(budget);
        }
    }
}
```

**3. WebSocket Push**
```java
@Controller
public class BudgetAlertWebSocketController {
    
    @MessageMapping("/budgets/alerts")
    @SendToUser("/queue/alerts")
    public BudgetAlertMessage checkAlerts(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<BudgetAlert> alerts = budgetService.getAlerts(user);
        return new BudgetAlertMessage(alerts);
    }
}
```

**Q10: How to implement budget forecasting?**

**A:** Predict when budget will be exceeded:

```java
public BudgetForecast forecastBudget(Budget budget) {
    
    // Get spending so far this month
    LocalDate now = LocalDate.now();
    int daysPassed = now.getDayOfMonth();
    BigDecimal currentSpending = calculateCurrentSpending(budget);
    
    // Calculate daily average
    BigDecimal dailyAverage = currentSpending
        .divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);
    
    // Calculate days in month
    int daysInMonth = YearMonth.of(budget.getYear(), budget.getMonth())
        .lengthOfMonth();
    
    // Project end-of-month spending
    BigDecimal projectedSpending = dailyAverage
        .multiply(BigDecimal.valueOf(daysInMonth));
    
    // Determine if will exceed
    boolean willExceed = projectedSpending.compareTo(budget.getLimitAmount()) > 0;
    
    // Calculate date when will exceed
    LocalDate exceedDate = null;
    if (willExceed) {
        BigDecimal remaining = budget.getLimitAmount().subtract(currentSpending);
        int daysToExceed = remaining.divide(dailyAverage, 0, RoundingMode.UP)
            .intValue();
        exceedDate = now.plusDays(daysToExceed);
    }
    
    return BudgetForecast.builder()
        .currentSpending(currentSpending)
        .projectedSpending(projectedSpending)
        .willExceed(willExceed)
        .exceedDate(exceedDate)
        .dailyAverage(dailyAverage)
        .build();
}
```

**Example:**
```
Budget: $3000 for October
Today: October 10
Spent so far: $1200
Daily average: $1200 / 10 = $120/day
Projected: $120 × 31 = $3720
Will exceed: Yes
Exceed date: Oct 10 + (1800 / 120) = Oct 25
Message: "At current spending rate, you'll exceed budget on Oct 25"
```

---

## Performance Optimization

### 1. N+1 Query Problem

**Current Implementation:**
```java
// 1 query: Get budgets
List<Budget> budgets = repository.findByUserIdAndMonthAndYear(...);

// N queries: Calculate spending for each
for (Budget budget : budgets) {
    BigDecimal spending = calculateCurrentSpending(budget);  // Queries DB
}
```

**Optimized Version:**
```java
// 1 query: Get budgets
List<Budget> budgets = repository.findByUserIdAndMonthAndYear(...);

// 1 query: Get all transactions for month
LocalDate start = ...;
LocalDate end = ...;
List<Transaction> allTransactions = transactionRepository
    .findByUserIdAndTransactionDateBetween(userId, start, end);

// Group by category in memory
Map<Long, BigDecimal> spendingByCategory = allTransactions.stream()
    .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
    .collect(Collectors.groupingBy(
        t -> t.getCategory().getId(),
        Collectors.reducing(
            BigDecimal.ZERO,
            Transaction::getAmount,
            BigDecimal::add
        )
    ));

// Map to budgets
for (Budget budget : budgets) {
    BigDecimal spending = spendingByCategory.getOrDefault(
        budget.getCategory().getId(), 
        BigDecimal.ZERO
    );
    // Use spending
}
```

### 2. Caching Strategy

```java
@Service
public class BudgetService {
    
    @Cacheable(
        value = "budget-spending",
        key = "#budget.id + '-' + T(java.time.LocalDate).now().toString()"
    )
    public BigDecimal calculateCurrentSpending(Budget budget) {
        // Cached per budget per day
    }
    
    @CacheEvict(
        value = "budget-spending",
        allEntries = true
    )
    public Transaction createTransaction(Transaction tx) {
        // Invalidate all cached spending when transaction created
    }
}
```

**Configuration:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
```

---

## Best Practices

### 1. BigDecimal Best Practices

```java
// ✅ GOOD: Use String constructor
BigDecimal amount = new BigDecimal("10.50");

// ❌ BAD: Use double constructor (precision loss)
BigDecimal amount = new BigDecimal(10.50);  // Might be 10.49999999...

// ✅ GOOD: Specify scale and rounding mode
BigDecimal result = a.divide(b, 2, RoundingMode.HALF_UP);

// ❌ BAD: No scale specified (exception if non-terminating decimal)
BigDecimal result = a.divide(b);  // Throws ArithmeticException

// ✅ GOOD: Use compareTo for comparison
if (a.compareTo(b) > 0) { ... }

// ❌ BAD: Use equals (checks scale too)
if (a.equals(b)) { ... }  // 10.0 != 10.00 with equals!
```

### 2. Date Handling

```java
// ✅ GOOD: Use YearMonth for month operations
YearMonth yearMonth = YearMonth.of(2025, 10);
LocalDate start = yearMonth.atDay(1);
LocalDate end = yearMonth.atEndOfMonth();

// ❌ BAD: Manual calculation (error-prone)
LocalDate start = LocalDate.of(2025, 10, 1);
LocalDate end = LocalDate.of(2025, 10, 31);  // Wrong for Feb!
```

### 3. Validation

```java
// ✅ GOOD: Multi-layer validation
@NotNull
@Min(1) @Max(12)
private Integer month;  // DTO validation

public void createBudget(...) {
    if (month < 1 || month > 12) {  // Service validation
        throw new IllegalArgumentException(...);
    }
}

@Column(nullable = false)
private Integer month;  // Database constraint
```

---

## Summary

**Phase 5 Key Concepts:**
- ✅ Composite unique constraints
- ✅ On-demand vs stored calculations
- ✅ BigDecimal for money
- ✅ Threshold-based alerts
- ✅ Month boundary handling
- ✅ Percentage calculations
- ✅ N+1 query awareness

**Key Takeaways for Interviews:**
1. Understand trade-offs (calculate vs store)
2. Explain BigDecimal necessity
3. Know how to optimize N+1 queries
4. Describe alert threshold selection
5. Discuss concurrent update handling

---
