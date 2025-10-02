# 📚 PHASE 4 - CRUD Operations & Advanced Filtering

## Complete Explanation for Learning & Interviews

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Components Deep Dive](#components-deep-dive)
4. [Security & Authorization](#security--authorization)
5. [Query Strategies](#query-strategies)
6. [Interview Questions & Answers](#interview-questions--answers)
7. [Best Practices](#best-practices)

---

## Overview

### What We Built

Phase 4 implements **complete CRUD operations** for Transactions and Categories with:
- Full Create, Read, Update, Delete for Transactions
- Category management with default categories
- Advanced filtering (date range, category)
- User authorization and data isolation
- Global exception handling
- Consistent error responses

### Why This Architecture?

| Pattern | Benefit |
|---------|---------|
| **Layered Architecture** | Separation of concerns, testability |
| **DTOs** | Decouple API from domain model |
| **Service Layer** | Centralize business logic |
| **Global Exception Handler** | Consistent error responses |
| **Repository Pattern** | Abstract data access |

---

## Architecture Patterns

### Layered Architecture Flow

```
┌─────────────┐
│   CLIENT    │ (Browser, Mobile App, Postman)
└──────┬──────┘
       │ HTTP Request (JSON)
       ▼
┌─────────────────────────────────────┐
│     JWT FILTER                      │ Validates token,
│     (Phase 3)                       │ sets SecurityContext
└──────┬──────────────────────────────┘
       ▼
┌─────────────────────────────────────┐
│     CONTROLLER LAYER                │ HTTP handling,
│  - TransactionController            │ DTO validation,
│  - CategoryController               │ user extraction
└──────┬──────────────────────────────┘
       ▼
┌─────────────────────────────────────┐
│     SERVICE LAYER                   │ Business logic,
│  - TransactionService               │ authorization,
│  - CategoryService                  │ data transformation
└──────┬──────────────────────────────┘
       ▼
┌─────────────────────────────────────┐
│     REPOSITORY LAYER                │ Database queries,
│  - TransactionRepository            │ JPA operations
│  - CategoryRepository               │
└──────┬──────────────────────────────┘
       ▼
┌─────────────────────────────────────┐
│     DATABASE                        │ MySQL
│  - Transactions, Categories, Users  │
└─────────────────────────────────────┘

       │ (If exception occurs)
       ▼
┌─────────────────────────────────────┐
│  GLOBAL EXCEPTION HANDLER           │ Catches exceptions,
│  (@ControllerAdvice)                │ returns error JSON
└─────────────────────────────────────┘
```

---

## Components Deep Dive

### 1. Custom Exceptions

#### Why Custom Exceptions?

**Problem:** Generic exceptions don't convey business context
```java
// BAD
throw new RuntimeException("Not found");
```

**Solution:** Domain-specific exceptions
```java
// GOOD
throw new ResourceNotFoundException("Transaction", "id", 5);
```

#### ResourceNotFoundException

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
}
```

**Usage:**
```java
Transaction tx = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
```

**Generates:** "Transaction not found with id: '5'"

#### UnauthorizedAccessException

**Purpose:** Prevent users from accessing others' data

```java
if (!transaction.getUser().getId().equals(currentUser.getId())) {
    throw new UnauthorizedAccessException("You can only update your own transactions");
}
```

**Returns:** 403 Forbidden (not 404, which would leak existence)

#### DuplicateResourceException

**Purpose:** Prevent duplicate resources

```java
if (userCategories.stream().anyMatch(c -> c.getName().equals(newName))) {
    throw new DuplicateResourceException("Category already exists: " + newName);
}
```

**Returns:** 409 Conflict

---

### 2. DTOs (Data Transfer Objects)

#### Why DTOs?

**Problem:** Exposing entities directly
```java
// BAD
@GetMapping
public List<Transaction> getAll() {
    return repository.findAll(); // Exposes internal structure!
}
```

**Issues:**
1. **Security:** Might expose password hashes, internal IDs
2. **Coupling:** API tied to database schema
3. **Performance:** Lazy-loaded collections cause N+1 queries
4. **Flexibility:** Can't customize response structure

**Solution:** Use DTOs
```java
// GOOD
@GetMapping
public List<TransactionResponse> getAll() {
    return transactions.stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
}
```

#### TransactionRequest vs TransactionResponse

**Request DTO (What we receive):**
```java
public class TransactionRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;              // Just the ID
    
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    
    // ... other fields
}
```

**Response DTO (What we send):**
```java
public class TransactionResponse {
    private Long id;                      // Include ID
    private Long categoryId;
    private String categoryName;          // Include name for convenience
    private LocalDateTime createdAt;      // Include timestamps
    private LocalDateTime updatedAt;
    // ... other fields
}
```

**Why Different?**
- **Request:** Client provides minimal data
- **Response:** Server provides enriched data (IDs, timestamps, related names)

#### Validation Annotations

```java
@NotNull(message = "Amount is required")                    // Cannot be null
@DecimalMin(value = "0.01", message = "Must be positive")   // Minimum value
@Digits(integer = 10, fraction = 2)                         // Precision control
@Size(max = 255)                                            // String length
@NotBlank                                                   // Not null, not empty
@Email                                                      // Email format
```

**Interview Question:** *"What's the difference between @NotNull and @NotBlank?"*
- **@NotNull:** Checks if value is not null (works for any type)
- **@NotEmpty:** Not null and not empty (for collections/strings)
- **@NotBlank:** Not null, not empty, and not just whitespace (strings only)

```java
String test = "   ";
@NotNull    → VALID
@NotEmpty   → VALID
@NotBlank   → INVALID (whitespace only)
```

---

### 3. Service Layer

#### TransactionService

**Purpose:** Centralize business logic and authorization

##### Create Transaction Flow

```java
@Transactional
public TransactionResponse createTransaction(TransactionRequest request, User currentUser) {
    
    // 1. Validate category exists
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
    
    // 2. Check authorization (can user use this category?)
    if (category.getUser() != null && !category.getUser().getId().equals(currentUser.getId())) {
        throw new UnauthorizedAccessException("You can only use your own categories or default categories");
    }
    
    // 3. Create entity from DTO
    Transaction transaction = new Transaction();
    transaction.setUser(currentUser);
    transaction.setCategory(category);
    transaction.setAmount(request.getAmount());
    // ... set other fields
    
    // 4. Save to database
    Transaction saved = transactionRepository.save(transaction);
    
    // 5. Convert entity to response DTO
    return mapToResponse(saved);
}
```

**Why @Transactional?**
- Database operations wrapped in transaction
- If any step fails, entire operation rolls back
- Example: If category validation passes but save() fails, no partial data

##### Update Transaction - Authorization Pattern

```java
public TransactionResponse updateTransaction(Long id, TransactionRequest request, User currentUser) {
    
    // 1. Find transaction
    Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
    
    // 2. CRITICAL: Verify ownership BEFORE update
    if (!transaction.getUser().getId().equals(currentUser.getId())) {
        throw new UnauthorizedAccessException("You can only update your own transactions");
    }
    
    // 3. Now safe to proceed with update
    transaction.setAmount(request.getAmount());
    // ... update other fields
    
    return mapToResponse(transactionRepository.save(transaction));
}
```

**Interview Deep Dive:**

**Q: "Why check ownership in service, not in repository query?"**

**A:** Multiple reasons:
1. **Explicit authorization:** Clear security checkpoint
2. **Better error messages:** Can distinguish between "not found" vs "not authorized"
3. **Testability:** Easy to unit test authorization logic
4. **Flexibility:** Can have complex authorization rules

**Alternative approach (less recommended):**
```java
// In repository
Optional<Transaction> findByIdAndUserId(Long id, Long userId);

// In service
Transaction transaction = repository.findByIdAndUserId(id, currentUser.getId())
    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
```

**Problem:** Can't distinguish between:
- Transaction doesn't exist
- Transaction exists but belongs to another user

**For security:** Different HTTP status codes matter!
- **404:** Resource doesn't exist (information leak if used for authorization)
- **403:** Resource exists but access denied (correct for authorization)

##### Filtering Strategy

```java
public List<TransactionResponse> getAllTransactions(
        User currentUser, LocalDate startDate, LocalDate endDate, Long categoryId) {
    
    List<Transaction> transactions;
    
    if (startDate != null && endDate != null && categoryId != null) {
        // Both filters: Query by date, filter by category in-memory
        transactions = repository.findByUserIdAndTransactionDateBetween(
            currentUser.getId(), startDate, endDate)
            .stream()
            .filter(t -> t.getCategory().getId().equals(categoryId))
            .collect(Collectors.toList());
            
    } else if (startDate != null && endDate != null) {
        // Date filter only
        transactions = repository.findByUserIdAndTransactionDateBetween(
            currentUser.getId(), startDate, endDate);
            
    } else if (categoryId != null) {
        // Category filter only
        transactions = repository.findByUserIdAndCategoryId(
            currentUser.getId(), categoryId);
            
    } else {
        // No filters
        transactions = repository.findByUserIdOrderByTransactionDateDesc(
            currentUser.getId());
    }
    
    return transactions.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
}
```

**Interview Question:** *"Why not create one complex query for all filter combinations?"*

**Answer:**
"We use a hybrid approach:

**Current Approach:**
- **Pros:**
  - Simple, readable code
  - Spring Data JPA generates queries automatically
  - Good performance for small datasets
  - Easy to maintain and test

- **Cons:**
  - In-memory filtering for combined filters
  - Multiple queries for different filter combinations

**Alternative (Complex Query):**
```java
@Query(\"SELECT t FROM Transaction t WHERE t.user.id = :userId \"
    + \"AND (:categoryId IS NULL OR t.category.id = :categoryId) \"
    + \"AND (:startDate IS NULL OR t.transactionDate >= :startDate) \"
    + \"AND (:endDate IS NULL OR t.transactionDate <= :endDate) \"
    + \"ORDER BY t.transactionDate DESC\")
List<Transaction> findWithFilters(
    @Param(\"userId\") Long userId,
    @Param(\"categoryId\") Long categoryId,
    @Param(\"startDate\") LocalDate startDate,
    @Param(\"endDate\") LocalDate endDate
);
```

**When to switch:**
- Dataset grows beyond ~10,000 transactions per user
- Performance metrics show in-memory filtering is slow
- Database has better query optimization

**Current decision:** Premature optimization is root of all evil. Start simple, optimize when needed."

---

#### CategoryService

##### @PostConstruct Pattern

```java
@PostConstruct
@Transactional
public void initializeDefaultCategories() {
    
    // Check if already initialized
    List<Category> existing = categoryRepository.findByIsDefaultTrue();
    if (!existing.isEmpty()) {
        log.info("Default categories already exist, skipping initialization");
        return;
    }
    
    // Create default categories
    List<Category> defaults = new ArrayList<>();
    defaults.add(createDefaultCategory("Salary", TransactionType.INCOME));
    defaults.add(createDefaultCategory("Food & Dining", TransactionType.EXPENSE));
    // ... 14 more categories
    
    categoryRepository.saveAll(defaults);
    log.info("Initialized {} default categories", defaults.size());
}
```

**@PostConstruct Explained:**
- Runs **after** bean creation and dependency injection
- Runs **before** application starts accepting requests
- Perfect for initialization tasks

**Interview Question:** *"What happens if app restarts?"*

**Answer:**
"The check prevents duplicates:
```java
if (!existing.isEmpty()) {
    return; // Skip if already initialized
}
```

**Why This Works:**
- First startup: Database empty → creates categories
- Subsequent restarts: Categories exist → skips creation
- Database is persistent, initialization is idempotent

**Alternative approaches:**
1. **Database migration (Flyway/Liquibase):** Better for production
2. **SQL script:** Run once manually
3. **Admin endpoint:** Create on-demand"

##### Default vs Custom Categories

```java
// Default Category
Category defaultCat = new Category();
defaultCat.setName("Salary");
defaultCat.setUser(null);         // No user association!
defaultCat.setIsDefault(true);

// Custom Category
Category customCat = new Category();
customCat.setName("Cryptocurrency");
customCat.setUser(currentUser);   // Belongs to specific user
customCat.setIsDefault(false);
```

**Query Strategy:**
```java
// Get user's categories = default + custom
List<Category> defaults = categoryRepository.findByIsDefaultTrue();  // WHERE is_default = true
List<Category> customs = categoryRepository.findByUserId(userId);    // WHERE user_id = ?

List<Category> all = new ArrayList<>();
all.addAll(defaults);  // Available to all users
all.addAll(customs);   // User's personal categories
```

---

### 4. Controller Layer

#### Extracting Current User

```java
@GetMapping
public ResponseEntity<List<TransactionResponse>> getAllTransactions(Authentication authentication) {
    
    // Get current user from SecurityContext
    User currentUser = getCurrentUser(authentication);
    
    // Pass to service
    List<TransactionResponse> transactions = service.getAllTransactions(currentUser, null, null, null);
    
    return ResponseEntity.ok(transactions);
}

private User getCurrentUser(Authentication authentication) {
    String email = authentication.getName();  // Email is the "username"
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
}
```

**How Authentication Object Works:**
1. JWT filter extracts email from token
2. Loads UserDetails from database
3. Creates Authentication object
4. Stores in SecurityContext (thread-local)
5. Spring injects into controller method

**Interview Scenario:** *"User sends request with stolen JWT token"*

**Answer:**
"JWT tokens contain user email but no password:
```
{
  \"sub\": \"john@example.com\",
  \"iat\": 1696253400,
  \"exp\": 1696339800
}
```

**If stolen:**
- Token is valid until expiration (24 hours)
- Attacker can make requests as that user
- Cannot change email in token (signature validation fails)

**Mitigations:**
1. **Short expiration:** 15 min - 1 hour (use refresh tokens)
2. **HTTPS only:** Prevents interception
3. **Token blacklist:** Store revoked tokens in Redis
4. **Device fingerprinting:** Detect unusual devices
5. **Refresh token rotation:** Invalidate old tokens
6. **Logout endpoint:** Add token to blacklist"

#### Query Parameters

```java
@GetMapping
public ResponseEntity<List<TransactionResponse>> getAllTransactions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long categoryId,
        Authentication authentication) {
    
    User currentUser = getCurrentUser(authentication);
    List<TransactionResponse> transactions = service.getAllTransactions(
        currentUser, startDate, endDate, categoryId
    );
    return ResponseEntity.ok(transactions);
}
```

**Annotations Explained:**
- `@RequestParam`: Extract query parameter from URL
- `required = false`: Parameter is optional
- `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`: Parse "2025-09-30" to LocalDate

**URL Examples:**
```
GET /api/transactions                                              → All transactions
GET /api/transactions?startDate=2025-09-01&endDate=2025-09-30    → September only
GET /api/transactions?categoryId=1                                → Salary category only
GET /api/transactions?startDate=2025-09-01&endDate=2025-09-30&categoryId=1  → Both filters
```

---

### 5. Global Exception Handler

#### @ControllerAdvice Pattern

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, 
            WebRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Resource Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(new ArrayList<>())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    // ... other exception handlers
}
```

**How It Works:**
1. Exception thrown anywhere in application
2. Spring catches exception
3. Finds matching @ExceptionHandler method
4. Calls handler method
5. Returns ResponseEntity with error details

**Interview Question:** *"Why @ControllerAdvice instead of try-catch in controllers?"*

**Answer:**
"**DRY Principle:** Don't Repeat Yourself

**Without @ControllerAdvice (BAD):**
```java
@GetMapping(\"/{id}\")
public ResponseEntity<?> get(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(service.get(id));
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(...);
    } catch (UnauthorizedAccessException e) {
        return ResponseEntity.status(403).body(...);
    }
}
```
**Problem:** Repeat try-catch in EVERY controller method!

**With @ControllerAdvice (GOOD):**
```java
@GetMapping(\"/{id}\")
public ResponseEntity<TransactionResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.get(id)); // Exception handled automatically
}
```

**Benefits:**
1. **Centralized:** One place for all error handling
2. **Consistent:** All errors follow same format
3. **Clean:** Controllers focus on happy path
4. **Maintainable:** Change error format in one place"

#### Validation Error Handling

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex, 
        WebRequest request) {
    
    // Extract field errors
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.toList());
    
    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Invalid input data")
        .path(request.getDescription(false).replace("uri=", ""))
        .details(errors)  // ["Amount is required", "Category ID is required"]
        .build();
    
    return ResponseEntity.badRequest().body(errorResponse);
}
```

**Example Response:**
```json
{
    "timestamp": "2025-10-02T11:30:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "Invalid input data",
    "path": "/api/transactions",
    "details": [
        "Amount is required",
        "Category ID is required",
        "Transaction date is required"
    ]
}
```

---

## Security & Authorization

### Multi-Level Security

```
┌─────────────────────────────────────────────────┐
│  Level 1: Authentication (JWT Filter)          │
│  - Is user logged in?                           │
│  - Is JWT token valid?                          │
│  Result: Sets SecurityContext or rejects (401)  │
└───────────────┬─────────────────────────────────┘
                ▼
┌─────────────────────────────────────────────────┐
│  Level 2: Endpoint Authorization (Security      │
│  Config)                                        │
│  - Is endpoint public or protected?             │
│  - /api/auth/** → permitAll()                   │
│  - /api/transactions/** → authenticated()       │
│  Result: Allow or reject (403)                  │
└───────────────┬─────────────────────────────────┘
                ▼
┌─────────────────────────────────────────────────┐
│  Level 3: Resource Authorization (Service)      │
│  - Does user own this resource?                 │
│  - transaction.user.id == currentUser.id?       │
│  Result: Proceed or throw exception (403)       │
└─────────────────────────────────────────────────┘
```

### Resource Ownership Pattern

**Always include user in query:**
```java
// DON'T: Allows accessing others' data
Transaction tx = repository.findById(id).orElseThrow(...);

// DO: Implicit authorization
List<Transaction> txs = repository.findByUserId(currentUser.getId());

// DO: Explicit authorization check
Transaction tx = repository.findById(id).orElseThrow(...);
if (!tx.getUser().getId().equals(currentUser.getId())) {
    throw new UnauthorizedAccessException("Access denied");
}
```

---

## Query Strategies

### Spring Data JPA Query Methods

#### Method Name Derivation

Spring generates queries from method names:

```java
// SELECT * FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC
List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

// SELECT * FROM transactions WHERE user_id = ? AND transaction_date BETWEEN ? AND ?
List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate start, LocalDate end);

// SELECT * FROM transactions WHERE user_id = ? AND category_id = ?
List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
```

**Naming Convention:**
- `find`: SELECT query
- `By`: WHERE clause starts
- `And`: Multiple conditions
- `Between`: Range query
- `OrderBy`: Sorting
- `Desc`: Descending order

#### When to Use @Query

**Use method names when:**
- Query is simple
- Follows standard patterns
- No complex joins

**Use @Query when:**
- Complex joins
- Aggregations
- Native SQL needed
- Performance tuning

**Example:**
```java
@Query("SELECT NEW com.finance.dto.MonthlyReport(MONTH(t.transactionDate), "
    + "SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), "
    + "SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END)) "
    + "FROM Transaction t WHERE t.user.id = :userId "
    + "GROUP BY MONTH(t.transactionDate)")
List<MonthlyReport> findMonthlyReport(@Param("userId") Long userId);
```

---

## Interview Questions & Answers

### Beginner Level

**Q1: What is the difference between Entity and DTO?**

**A:**
- **Entity:** Database table representation (JPA annotations, relationships)
- **DTO:** Data transfer between layers (validation, no JPA annotations)

**Why separate?**
1. **Security:** Don't expose internal structure
2. **Flexibility:** API can differ from database
3. **Performance:** Control what data is loaded/sent

**Q2: Why use BigDecimal for money?**

**A:**
```java
// WRONG: Float/Double
double price = 0.1 + 0.2;  // 0.30000000000000004 (rounding error!)

// CORRECT: BigDecimal
BigDecimal price = new BigDecimal("0.1").add(new BigDecimal("0.2"));  // 0.3
```

**Reasons:**
- Float/Double use binary representation (not exact for decimals)
- Financial calculations require exact precision
- `BigDecimal` uses decimal representation

**Q3: What is @Transactional?**

**A:** Wraps method in database transaction:
```java
@Transactional
public void transfer(Account from, Account to, BigDecimal amount) {
    from.debit(amount);   // Step 1
    to.credit(amount);    // Step 2
}
// If Step 2 fails, Step 1 is rolled back (ACID)
```

### Intermediate Level

**Q4: Explain N+1 query problem**

**A:**
```java
// BAD: N+1 queries
List<Transaction> transactions = repository.findAll();  // 1 query
for (Transaction tx : transactions) {
    String categoryName = tx.getCategory().getName();   // N queries (one per transaction!)
}
```

**Solution 1: JOIN FETCH**
```java
@Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user.id = :userId")
List<Transaction> findByUserIdWithCategory(@Param("userId") Long userId);
```

**Solution 2: Use DTOs**
```java
// Query once, map to DTO
List<TransactionResponse> responses = transactions.stream()
    .map(tx -> TransactionResponse.builder()
        .categoryName(tx.getCategory().getName())  // Already loaded
        .build())
    .collect(Collectors.toList());
```

**Q5: How does @Valid work?**

**A:**
1. Spring intercepts request
2. Deserializes JSON to DTO
3. Runs validation annotations
4. If validation fails, throws `MethodArgumentNotValidException`
5. GlobalExceptionHandler catches and returns 400

**Q6: Explain ResponseEntity vs @ResponseBody**

**A:**
```java
// @ResponseBody: Auto-serializes return value to JSON (implicit 200)
@ResponseBody
@GetMapping
public List<Transaction> getAll() {
    return service.getAll();
}

// ResponseEntity: Full control over status, headers, body
@GetMapping
public ResponseEntity<List<TransactionResponse>> getAll() {
    List<TransactionResponse> data = service.getAll();
    return ResponseEntity.ok()
        .header("X-Total-Count", String.valueOf(data.size()))
        .body(data);
}
```

### Advanced Level

**Q7: Implement soft delete for transactions**

**A:**
```java
// 1. Add field to entity
@Entity
public class Transaction {
    private Boolean deleted = false;
    private LocalDateTime deletedAt;
}

// 2. Update repository
@Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deleted = false")
List<Transaction> findActiveByUserId(@Param("userId") Long userId);

// 3. Update service
public void deleteTransaction(Long id, User currentUser) {
    Transaction tx = repository.findById(id).orElseThrow(...);
    // Authorize...
    tx.setDeleted(true);
    tx.setDeletedAt(LocalDateTime.now());
    repository.save(tx);  // Don't actually delete
}

// 4. Add restore endpoint
public void restore(Long id, User currentUser) {
    Transaction tx = repository.findById(id).orElseThrow(...);
    tx.setDeleted(false);
    tx.setDeletedAt(null);
    repository.save(tx);
}
```

**Q8: How to handle concurrent updates?**

**A:** Use optimistic locking:
```java
@Entity
public class Transaction {
    @Version
    private Long version;  // Auto-incremented by JPA
}

// When two users update same transaction:
// User A: version=1 → updates to version=2 ✓
// User B: version=1 → tries to update, but version is now 2 → OptimisticLockException
```

**In service:**
```java
try {
    repository.save(transaction);
} catch (OptimisticLockingFailureException e) {
    throw new ConflictException("Transaction was updated by another user. Please refresh and try again.");
}
```

**Q9: Implement pagination for transactions**

**A:**
```java
// 1. Repository extends PagingAndSortingRepository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUserId(Long userId, Pageable pageable);
}

// 2. Controller
@GetMapping
public ResponseEntity<Page<TransactionResponse>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "transactionDate,desc") String[] sort,
        Authentication auth) {
    
    // Create pageable
    Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
    Pageable pageable = PageRequest.of(page, size, sorting);
    
    // Get page
    Page<Transaction> pageResult = repository.findByUserId(getCurrentUser(auth).getId(), pageable);
    
    // Map to DTO
    Page<TransactionResponse> response = pageResult.map(this::mapToResponse);
    
    return ResponseEntity.ok(response);
}

// 3. Response includes metadata
{
    "content": [...],           // Transactions
    "pageable": {...},
    "totalPages": 5,
    "totalElements": 100,
    "last": false,
    "first": true,
    "size": 20,
    "number": 0
}
```

**Q10: How to implement search/filtering with complex criteria?**

**A:** Use Specifications (JPA Criteria API):
```java
// 1. Enable Specifications
public interface TransactionRepository extends JpaSpecificationExecutor<Transaction> {
}

// 2. Create Specification
public class TransactionSpecifications {
    
    public static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
    
    public static Specification<Transaction> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : 
            cb.equal(root.get("category").get("id"), categoryId);
    }
    
    public static Specification<Transaction> betweenDates(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null || end == null) return null;
            return cb.between(root.get("transactionDate"), start, end);
        };
    }
}

// 3. Use in service
public List<Transaction> findWithFilters(Long userId, LocalDate start, LocalDate end, Long categoryId) {
    Specification<Transaction> spec = Specification
        .where(TransactionSpecifications.hasUserId(userId))
        .and(TransactionSpecifications.hasCategory(categoryId))
        .and(TransactionSpecifications.betweenDates(start, end));
    
    return repository.findAll(spec);
}
```

---

## Best Practices

### 1. DTO Mapping

**Always map at service layer:**
```java
// GOOD
@Service
public class TransactionService {
    public TransactionResponse create(...) {
        Transaction saved = repository.save(transaction);
        return mapToResponse(saved);  // Service returns DTO
    }
}

// BAD
@Service
public class TransactionService {
    public Transaction create(...) {
        return repository.save(transaction);  // Service returns Entity
    }
}
```

### 2. Authorization

**Check ownership BEFORE operations:**
```java
// GOOD
Transaction tx = repository.findById(id).orElseThrow(...);
if (!tx.getUser().getId().equals(currentUser.getId())) {
    throw new UnauthorizedAccessException(...);
}
tx.setAmount(newAmount);
repository.save(tx);

// BAD
Transaction tx = repository.findById(id).orElseThrow(...);
tx.setAmount(newAmount);
repository.save(tx);  // No authorization check!
```

### 3. Error Messages

**Generic for security:**
```java
// GOOD
if (!userExists) {
    throw new BadCredentialsException("Invalid email or password");
}

// BAD
if (!userExists) {
    throw new UsernameNotFoundException("Email not found");  // Info leak!
}
```

### 4. Validation

**Multiple layers:**
```java
// 1. Bean Validation (DTO)
@NotNull @DecimalMin("0.01")
private BigDecimal amount;

// 2. Business Validation (Service)
if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("Amount must be positive");
}

// 3. Database Constraints (Entity)
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal amount;
```

---

## Summary

**Phase 4 Patterns:**
- ✅ Layered architecture (Controller → Service → Repository)
- ✅ DTOs for API contracts
- ✅ Service-layer authorization
- ✅ Custom exceptions for business logic
- ✅ Global exception handling
- ✅ Query method derivation
- ✅ @Transactional for consistency
- ✅ @PostConstruct for initialization

**Key Takeaways for Interviews:**
1. Understand why DTOs separate from Entities
2. Explain authorization vs authentication
3. Know when to use @Query vs method names
4. Describe N+1 problem and solutions
5. Discuss soft delete, pagination, optimistic locking

---

**Next:** Phase 5 will add budget tracking with alerts and spending calculations!


