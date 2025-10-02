# 📘 API Reference Guide

Complete reference for all API endpoints in the Personal Finance Tracker API.

---

## 🔐 Authentication

All endpoints except authentication require a JWT token in the Authorization header:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## Authentication Endpoints

### Register New User

**POST** `/api/auth/register`

Creates a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- `email` - Valid email format, unique
- `password` - Minimum 6 characters
- `firstName` - Not blank
- `lastName` - Not blank

**Success Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjk2MjU...",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

### Login

**POST** `/api/auth/login`

Authenticate existing user and receive JWT token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Success Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Error Response (401):**
```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials"
}
```

---

## Transaction Endpoints

All transaction endpoints require authentication.

### Create Transaction

**POST** `/api/transactions`

Create a new income or expense transaction.

**Request Body:**
```json
{
  "categoryId": 1,
  "amount": 1500.50,
  "description": "Grocery shopping at Whole Foods",
  "transactionDate": "2025-10-02",
  "paymentMethod": "CREDIT_CARD",
  "transactionType": "EXPENSE"
}
```

**Field Details:**
- `categoryId` (required) - ID of category
- `amount` (required) - Must be positive, supports 2 decimal places
- `description` (optional) - Transaction notes
- `transactionDate` (required) - Date in YYYY-MM-DD format
- `paymentMethod` (required) - One of: `CASH`, `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `BANK_TRANSFER`
- `transactionType` (required) - One of: `INCOME`, `EXPENSE`

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "amount": 1500.50,
  "description": "Grocery shopping at Whole Foods",
  "transactionDate": "2025-10-02",
  "paymentMethod": "CREDIT_CARD",
  "transactionType": "EXPENSE",
  "createdAt": "2025-10-02T10:30:00"
}
```

---

### Get All Transactions

**GET** `/api/transactions`

Retrieve all transactions for the authenticated user with optional filters.

**Query Parameters (all optional):**
- `startDate` - Filter from this date (YYYY-MM-DD)
- `endDate` - Filter until this date (YYYY-MM-DD)
- `categoryId` - Filter by category ID
- `type` - Filter by type (`INCOME` or `EXPENSE`)

**Examples:**
```
GET /api/transactions
GET /api/transactions?startDate=2025-10-01&endDate=2025-10-31
GET /api/transactions?categoryId=1
GET /api/transactions?type=EXPENSE
GET /api/transactions?startDate=2025-10-01&categoryId=1&type=EXPENSE
```

**Success Response (200):**
```json
[
  {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Food & Dining",
    "amount": 1500.50,
    "description": "Grocery shopping",
    "transactionDate": "2025-10-02",
    "paymentMethod": "CREDIT_CARD",
    "transactionType": "EXPENSE",
    "createdAt": "2025-10-02T10:30:00"
  },
  {
    "id": 2,
    "categoryId": 8,
    "categoryName": "Salary",
    "amount": 50000.00,
    "description": "Monthly salary",
    "transactionDate": "2025-10-01",
    "paymentMethod": "BANK_TRANSFER",
    "transactionType": "INCOME",
    "createdAt": "2025-10-01T09:00:00"
  }
]
```

---

### Get Transaction by ID

**GET** `/api/transactions/{id}`

Retrieve a specific transaction.

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "amount": 1500.50,
  "description": "Grocery shopping",
  "transactionDate": "2025-10-02",
  "paymentMethod": "CREDIT_CARD",
  "transactionType": "EXPENSE",
  "createdAt": "2025-10-02T10:30:00"
}
```

**Error Response (404):**
```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found with id: 1"
}
```

---

### Update Transaction

**PUT** `/api/transactions/{id}`

Update an existing transaction. Only the transaction owner can update.

**Request Body:**
```json
{
  "categoryId": 1,
  "amount": 1650.75,
  "description": "Updated description",
  "transactionDate": "2025-10-02",
  "paymentMethod": "DEBIT_CARD",
  "transactionType": "EXPENSE"
}
```

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "amount": 1650.75,
  "description": "Updated description",
  "transactionDate": "2025-10-02",
  "paymentMethod": "DEBIT_CARD",
  "transactionType": "EXPENSE",
  "createdAt": "2025-10-02T10:30:00"
}
```

---

### Delete Transaction

**DELETE** `/api/transactions/{id}`

Delete a transaction. Only the transaction owner can delete.

**Success Response (200):**
```
Transaction deleted successfully
```

**Error Response (403):**
```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to delete this transaction"
}
```

---

## Category Endpoints

### Get All Categories

**GET** `/api/categories`

Retrieve all categories available to the user (default + custom).

**Success Response (200):**
```json
[
  {
    "id": 1,
    "name": "Food & Dining",
    "type": "EXPENSE",
    "isDefault": true
  },
  {
    "id": 2,
    "name": "Transportation",
    "type": "EXPENSE",
    "isDefault": true
  },
  {
    "id": 15,
    "name": "Pet Expenses",
    "type": "EXPENSE",
    "isDefault": false
  }
]
```

**Default Categories (Auto-created):**

*Expense Categories:*
- Food & Dining
- Transportation
- Entertainment
- Bills & Utilities
- Shopping
- Healthcare

*Income Categories:*
- Salary
- Freelance
- Business
- Investments
- Other Income

---

### Create Custom Category

**POST** `/api/categories`

Create a custom category for the authenticated user.

**Request Body:**
```json
{
  "name": "Pet Expenses",
  "type": "EXPENSE"
}
```

**Field Details:**
- `name` (required) - Category name (max 50 characters)
- `type` (required) - One of: `INCOME`, `EXPENSE`

**Success Response (200):**
```json
{
  "id": 15,
  "name": "Pet Expenses",
  "type": "EXPENSE",
  "isDefault": false
}
```

**Error Response (409):**
```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Category already exists with name: Pet Expenses"
}
```

---

## Budget Endpoints

### Create Budget

**POST** `/api/budgets`

Set a monthly budget for a specific category.

**Request Body:**
```json
{
  "categoryId": 1,
  "limitAmount": 5000,
  "month": 10,
  "year": 2025
}
```

**Field Details:**
- `categoryId` (required) - Category to budget for
- `limitAmount` (required) - Budget limit (positive number)
- `month` (required) - Month number (1-12)
- `year` (required) - Year (e.g., 2025)

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "limitAmount": 5000.00,
  "currentSpending": 2350.50,
  "percentageUsed": 47.01,
  "month": 10,
  "year": 2025,
  "createdAt": "2025-10-02T10:30:00"
}
```

**Error Response (409):**
```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Budget already exists for Food & Dining in October 2025"
}
```

---

### Get Budgets

**GET** `/api/budgets`

Retrieve budgets for a specific month and year.

**Query Parameters (both required):**
- `month` - Month number (1-12)
- `year` - Year (e.g., 2025)

**Example:**
```
GET /api/budgets?month=10&year=2025
```

**Success Response (200):**
```json
[
  {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Food & Dining",
    "limitAmount": 5000.00,
    "currentSpending": 2350.50,
    "percentageUsed": 47.01,
    "month": 10,
    "year": 2025,
    "createdAt": "2025-10-02T10:30:00"
  },
  {
    "id": 2,
    "categoryId": 2,
    "categoryName": "Transportation",
    "limitAmount": 3000.00,
    "currentSpending": 2700.00,
    "percentageUsed": 90.00,
    "month": 10,
    "year": 2025,
    "createdAt": "2025-10-01T08:00:00"
  }
]
```

---

### Get Budget by ID

**GET** `/api/budgets/{id}`

Retrieve a specific budget with current spending.

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "limitAmount": 5000.00,
  "currentSpending": 2350.50,
  "percentageUsed": 47.01,
  "month": 10,
  "year": 2025,
  "createdAt": "2025-10-02T10:30:00"
}
```

---

### Update Budget

**PUT** `/api/budgets/{id}`

Update an existing budget.

**Request Body:**
```json
{
  "categoryId": 1,
  "limitAmount": 6000,
  "month": 10,
  "year": 2025
}
```

**Success Response (200):**
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Food & Dining",
  "limitAmount": 6000.00,
  "currentSpending": 2350.50,
  "percentageUsed": 39.18,
  "month": 10,
  "year": 2025,
  "createdAt": "2025-10-02T10:30:00"
}
```

---

### Delete Budget

**DELETE** `/api/budgets/{id}`

Delete a budget.

**Success Response (200):**
```
Budget deleted successfully
```

---

### Get Budget Alerts

**GET** `/api/budgets/alerts`

Get alerts for budgets exceeding 80% of limit (current month only).

**Success Response (200):**
```json
[
  {
    "budgetId": 2,
    "categoryName": "Transportation",
    "limitAmount": 3000.00,
    "currentSpending": 2700.00,
    "percentageUsed": 90.00,
    "daysLeftInMonth": 15,
    "alertLevel": "WARNING",
    "message": "90% of budget used with 15 days remaining"
  },
  {
    "budgetId": 3,
    "categoryName": "Entertainment",
    "limitAmount": 2000.00,
    "currentSpending": 2150.00,
    "percentageUsed": 107.50,
    "daysLeftInMonth": 15,
    "alertLevel": "DANGER",
    "message": "Budget exceeded by 7.50%"
  }
]
```

**Alert Levels:**
- `WARNING` - 80-99% of budget used
- `DANGER` - 100%+ of budget used (exceeded)

---

## Error Responses

All errors follow a consistent format:

```json
{
  "timestamp": "2025-10-02T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: amount must be positive"
}
```

### HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success | Request completed successfully |
| 400 | Bad Request | Invalid input data or validation error |
| 401 | Unauthorized | Invalid or missing JWT token |
| 403 | Forbidden | User doesn't have permission |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., budget already exists) |
| 500 | Internal Server Error | Server error |

---

## Common Validation Rules

### Amount Fields
- Must be positive
- Maximum 2 decimal places
- Example: `1500.50`

### Date Fields
- Format: `YYYY-MM-DD`
- Example: `2025-10-02`

### Month Field
- Integer between 1-12
- Example: `10` for October

### Email Field
- Valid email format
- Example: `user@example.com`

---

## Rate Limiting

Currently, no rate limiting is implemented. In production, consider:
- 100 requests per minute per user
- Implement using Spring's `@RateLimiter` or Redis

---

## Data Types Reference

### PaymentMethod Enum
```
CASH
CREDIT_CARD
DEBIT_CARD
UPI
BANK_TRANSFER
```

### TransactionType Enum
```
INCOME
EXPENSE
```

### CategoryType Enum
```
INCOME
EXPENSE
```

### Alert Level
```
WARNING  (80-99% of budget)
DANGER   (100%+ of budget)
```

---

## Testing with cURL

### Complete Workflow Example

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123","firstName":"Test","lastName":"User"}'

# 2. Login (save the token)
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123"}' \
  | jq -r '.token')

# 3. Get categories
curl -X GET http://localhost:8080/api/categories \
  -H "Authorization: Bearer $TOKEN"

# 4. Create transaction
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"categoryId":1,"amount":1500,"description":"Test","transactionDate":"2025-10-02","paymentMethod":"CASH","transactionType":"EXPENSE"}'

# 5. Create budget
curl -X POST http://localhost:8080/api/budgets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"categoryId":1,"limitAmount":5000,"month":10,"year":2025}'

# 6. Get budget alerts
curl -X GET http://localhost:8080/api/budgets/alerts \
  -H "Authorization: Bearer $TOKEN"
```

---

## Testing with PowerShell

```powershell
# Login and get token
$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"pass123"}'

$token = $loginResponse.token
$headers = @{ "Authorization" = "Bearer $token" }

# Get transactions
Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" `
  -Method GET `
  -Headers $headers
```

---

**Last Updated:** October 2025  
**API Version:** 1.0  
**Base URL:** `http://localhost:8080`

