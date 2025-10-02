# 💰 Personal Finance Tracker API

A robust REST API for managing personal finances built with Spring Boot. Track expenses, set budgets, and gain insights into your spending patterns - all secured with JWT authentication.

> **Built with:** Java 17 • Spring Boot 3.2 • MySQL • JWT Authentication

---

## 🎯 What This Project Demonstrates

This project showcases proficiency in:

- **Spring Boot 3.x** - Modern Java backend development
- **Spring Security + JWT** - Secure authentication & authorization
- **Spring Data JPA** - Database operations with Hibernate
- **RESTful API Design** - Clean, intuitive endpoints
- **MySQL Database** - Relational data modeling
- **Exception Handling** - Global error handling with custom exceptions
- **DTO Pattern** - Proper separation of concerns
- **Validation** - Input validation using Bean Validation
- **Lombok** - Cleaner code with less boilerplate

---

## ✨ Features

### 🔐 Authentication & Security
- User registration with encrypted passwords
- JWT-based authentication
- Secure endpoints with token validation
- Role-based access control

### 💸 Transaction Management
- Create, read, update, and delete transactions
- Track both income and expenses
- Categorize transactions (Food, Transport, Entertainment, etc.)
- Multiple payment methods (Cash, Card, UPI, Bank Transfer)
- Filter transactions by date range and category

### 📊 Budget Tracking
- Set monthly budgets per category
- Real-time spending calculation
- Budget alerts (80% warning, 100% exceeded)
- Automatic spending percentage tracking
- Duplicate budget prevention

### 🏷️ Category Management
- Pre-loaded default categories
- Create custom categories
- Separate income and expense categories
- Category-wise transaction grouping

---

## 🚀 Quick Start

### Prerequisites

Ensure you have installed:
- **Java 17+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [Download here](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** - [Download here](https://dev.mysql.com/downloads/)

### Database Setup

1. **Create the database:**
   ```sql
   CREATE DATABASE finance_tracker;
   ```

2. **Verify MySQL is running:**
   ```bash
   mysql -u root -p
   USE finance_tracker;
   ```

### Configuration

Update `src/main/resources/application.properties`:

```properties
# Database credentials
spring.datasource.password=your_mysql_password

# JWT Secret (change in production!)
jwt.secret=your-secret-key-here
```

### Run the Application

```bash
# Clone and navigate to project
cd Java-Personal-Finance-API

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

🎉 **Application running at:** `http://localhost:8080`

---

## 📚 API Documentation

### Interactive Swagger UI

Once running, access full API documentation at:

**→ http://localhost:8080/swagger-ui.html**

### Quick API Reference

#### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

#### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Create transaction |
| GET | `/api/transactions` | Get all transactions (with filters) |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

**Query Parameters:**
- `startDate` - Filter by start date (YYYY-MM-DD)
- `endDate` - Filter by end date (YYYY-MM-DD)
- `categoryId` - Filter by category

#### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | Get all categories (default + custom) |
| POST | `/api/categories` | Create custom category |

#### Budgets

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/budgets` | Create budget |
| GET | `/api/budgets` | Get budgets by month/year |
| GET | `/api/budgets/{id}` | Get budget by ID |
| PUT | `/api/budgets/{id}` | Update budget |
| DELETE | `/api/budgets/{id}` | Delete budget |
| GET | `/api/budgets/alerts` | Get budget alerts |

**Query Parameters:**
- `month` - Month (1-12)
- `year` - Year (e.g., 2025)

---

## 🧪 Example Requests

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePass123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePass123"
  }'
```

### 3. Create Transaction (requires JWT)

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "amount": 1500.50,
    "description": "Grocery shopping",
    "transactionDate": "2025-10-02",
    "paymentMethod": "CREDIT_CARD",
    "transactionType": "EXPENSE"
  }'
```

### 4. Set a Budget

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "limitAmount": 5000,
    "month": 10,
    "year": 2025
  }'
```

### 5. Get Budget Alerts

```bash
curl -X GET http://localhost:8080/api/budgets/alerts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
[
  {
    "budgetId": 1,
    "categoryName": "Food & Dining",
    "limitAmount": 3000.00,
    "currentSpending": 2550.00,
    "percentageUsed": 85.00,
    "daysLeftInMonth": 15,
    "alertLevel": "WARNING",
    "message": "85% of budget used with 15 days remaining"
  }
]
```

---

## 🏗️ Project Architecture

```
┌─────────────────────────────────────────────┐
│           REST Controllers                   │
│    (AuthController, TransactionController)   │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│           Service Layer                      │
│    (Business Logic & Calculations)           │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│         Repository Layer                     │
│      (Spring Data JPA Repositories)          │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│            MySQL Database                    │
│  (Users, Transactions, Budgets, Categories)  │
└─────────────────────────────────────────────┘
```

### Project Structure

```
src/main/java/com/finance/tracker/
├── config/              # Security & Application Configuration
├── controller/          # REST API Endpoints
├── dto/                # Data Transfer Objects
├── exception/          # Custom Exceptions & Global Handler
├── model/              # JPA Entities
├── repository/         # Data Access Layer
├── security/           # JWT & Security Components
└── service/            # Business Logic Layer
```

---

## 🗄️ Database Schema

### Key Tables

**Users**
- User authentication and profile information
- One-to-many with Transactions, Categories, Budgets

**Categories**
- Both system default and user-custom categories
- Tracks whether expense or income category

**Transactions**
- Core financial records (income/expenses)
- Links to user and category
- Supports multiple payment methods

**Budgets**
- Monthly spending limits per category
- Unique constraint: user + category + month + year
- Spending calculated on-demand from transactions

---

## 🛡️ Security Features

- **Password Encryption** - BCrypt hashing
- **JWT Tokens** - Stateless authentication (24-hour expiration)
- **Protected Endpoints** - All endpoints except `/api/auth/**` require authentication
- **User Isolation** - Users can only access their own data
- **Global Exception Handling** - Consistent error responses

---


## 📦 Tech Stack Details

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming Language |
| Spring Boot | 3.2.0 | Application Framework |
| Spring Security | 3.2.0 | Authentication & Authorization |
| Spring Data JPA | 3.2.0 | Database ORM |
| MySQL | 8.0+ | Relational Database |
| JWT (jjwt) | 0.12.3 | Token-based Authentication |
| Lombok | Latest | Reduce Boilerplate Code |
| SpringDoc OpenAPI | 2.2.0 | API Documentation (Swagger) |
| Maven | 3.6+ | Build & Dependency Management |

---

## 🔍 Testing

### Run Tests
```bash
mvn test
```

### Manual Testing with Swagger
1. Start the application
2. Navigate to http://localhost:8080/swagger-ui.html
3. Use the "Authorize" button to add your JWT token
4. Test endpoints directly from the UI

---

## 📈 What's Implemented (Phases Completed)

- ✅ **Phase 1:** Project Setup & Configuration
- ✅ **Phase 2:** Database Models & JPA Entities
- ✅ **Phase 3:** JWT Authentication & Security
- ✅ **Phase 4:** Transaction & Category CRUD
- ✅ **Phase 5:** Budget Management & Alerts

---

## 🚀 Future Enhancements

While the current implementation is feature-complete for a portfolio project, potential additions include:

- 📊 **Analytics Dashboard** - Spending trends, category breakdowns, insights
- 📧 **Email Notifications** - Budget alerts via email
- 📱 **Mobile App** - React Native or Flutter frontend
- 🔄 **Recurring Transactions** - Auto-create monthly bills
- 💱 **Multi-Currency Support** - Handle different currencies
- 📤 **Export Data** - Download transactions as CSV/PDF
- 🧪 **Comprehensive Testing** - Unit & integration tests

---

## 📝 Learning Resources

If you're new to these technologies, check out:

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security + JWT Tutorial](https://spring.io/guides/topicals/spring-security-architecture)
- [Spring Data JPA Guide](https://spring.io/guides/gs/accessing-data-jpa/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

---

## 📄 License

This project is created for educational and portfolio purposes.

---

## 🤝 Contributing

Feel free to fork this project and experiment! If you have suggestions or find issues, please open an issue or submit a pull request.


---

Made with Java ☕ and Spring Boot with 💖 by Mr.Nithesh
