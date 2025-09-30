# 💰 Personal Finance Tracker API

A comprehensive REST API for managing personal finances with expense/income tracking, budget management, and smart analytics.

## 🚀 Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** (Hibernate)
- **MySQL** (Database)
- **Maven** (Build Tool)
- **Lombok** (Boilerplate Reduction)
- **SpringDoc OpenAPI** (API Documentation/Swagger)

## 📋 Features (Planned)

- ✅ User Registration & JWT Authentication
- ✅ Transaction Management (Income/Expense tracking)
- ✅ Custom & Default Categories
- ✅ Budget Setting & Alerts
- ✅ Monthly Financial Summary
- ✅ Category-wise Spending Breakdown
- ✅ Spending Trends Analysis
- ✅ Smart Financial Insights
- ✅ Swagger API Documentation

## 🛠️ Prerequisites

Before running this application, ensure you have:

1. **Java 17** or higher installed
   ```bash
   java -version
   ```

2. **Maven 3.6+** installed
   ```bash
   mvn -version
   ```

3. **MySQL 8.0+** installed and running
   ```bash
   mysql --version
   ```

## 📦 Database Setup

1. **Start MySQL** (if not already running)

2. **Create the database:**
   ```sql
   CREATE DATABASE finance_tracker;
   ```

3. **Verify connection:**
   ```bash
   mysql -u root -p
   USE finance_tracker;
   ```

## ⚙️ Configuration

1. **Update `src/main/resources/application.properties`** with your database credentials:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/finance_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```

2. **JWT Secret** (Optional for development - Change in production):
   ```properties
   jwt.secret=your-secure-256-bit-secret-key
   ```

## 🚀 Running the Application

### Option 1: Using Maven

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Option 2: Using JAR

```bash
# Build JAR file
mvn clean package

# Run the JAR
java -jar target/finance-tracker-api-1.0.0.jar
```

The application will start on **http://localhost:8080**

## 📚 API Documentation

Once the application is running, access Swagger UI at:

**http://localhost:8080/swagger-ui.html**

OpenAPI JSON documentation:

**http://localhost:8080/v3/api-docs**

## 📁 Project Structure

```
finance-tracker-api/
├── src/
│   ├── main/
│   │   ├── java/com/finance/tracker/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST Controllers
│   │   │   ├── service/          # Business Logic
│   │   │   ├── repository/       # Data Access Layer
│   │   │   ├── model/            # JPA Entities
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── exception/        # Custom Exceptions
│   │   │   ├── security/         # Security & JWT
│   │   │   ├── util/             # Utility Classes
│   │   │   └── FinanceTrackerApiApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/finance/tracker/
├── pom.xml
└── README.md
```

## 🔑 Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.0 | Framework |
| Spring Security | 3.2.0 | Authentication |
| JWT (jjwt) | 0.12.3 | Token-based auth |
| MySQL Driver | Latest | Database |
| Lombok | Latest | Reduce boilerplate |
| SpringDoc OpenAPI | 2.2.0 | API Documentation |

## 🧪 Testing

Run all tests:
```bash
mvn test
```

## 📝 Development Progress

### ✅ Phase 1: Project Setup (COMPLETED)
- [x] Maven project structure
- [x] Dependencies configuration
- [x] Application properties
- [x] Package structure

### 🔄 Phase 2-7: Coming Soon
- [ ] Database Models & Entities
- [ ] Security & JWT Authentication
- [ ] Core CRUD Operations
- [ ] Budget Management
- [ ] Analytics & Insights
- [ ] Documentation & Testing

## 🤝 Contributing

This is a learning project. Feel free to fork and experiment!

## 📄 License

This project is created for educational purposes.

---

**Status:** Phase 1 Complete ✅ | Ready for Phase 2 Development
