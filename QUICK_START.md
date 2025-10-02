# 🚀 Quick Start Guide - Finance Tracker API

## ⚡ Running the Application (After Phase 2+)

### Prerequisites Check
```bash
# Check Java version (should be 17+)
java -version

# Check Maven version
mvn -version

# Check MySQL
mysql --version
```

---

## 🗄️ Database Setup

### Step 1: Start MySQL
```bash
# Windows (if installed as service)
# MySQL starts automatically

# Or start manually via Services
# Or use MySQL Workbench to start
```

### Step 2: Create Database
```bash
# Open MySQL command line
mysql -u root -p

# In MySQL prompt, run:
CREATE DATABASE finance_tracker;

# Verify database created
SHOW DATABASES;

# Exit MySQL
exit;
```

### Step 3: Update Configuration
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

---

## 🏃 Running the Application

### Method 1: Using Maven (Recommended for Development)
```bash
# Navigate to project directory
cd D:\GitHub\Java-Personal-Finance-API

# Run the application
mvn spring-boot:run
```

### Method 2: Using JAR File (Production-like)
```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/finance-tracker-api-1.0.0.jar
```

### Method 3: Using IDE (IntelliJ IDEA / Eclipse)
1. Open project in IDE
2. Right-click on `FinanceTrackerApiApplication.java`
3. Select "Run 'FinanceTrackerApiApplication'"

---

## ✅ Verify Application is Running

### Check Console Output
You should see:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

...
Started FinanceTrackerApiApplication in 3.456 seconds
```

### Check Application Health
Open browser: http://localhost:8080

You should see either:
- Swagger UI (after Phase 7)
- Login page (after Phase 3)
- 404 error (Phase 1-2 - normal, no endpoints yet)

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=FinanceTrackerApiApplicationTests
```

### Run with Coverage (Optional)
```bash
mvn test jacoco:report
# Report in: target/site/jacoco/index.html
```

---

## 📚 Accessing API Documentation (Phase 7)

### Swagger UI
**URL:** http://localhost:8080/swagger-ui.html

Features:
- Interactive API documentation
- Test endpoints directly from browser
- See request/response examples

### OpenAPI JSON
**URL:** http://localhost:8080/v3/api-docs

Use for:
- Importing into Postman
- Generating client SDKs
- API contract sharing

---

## 🛠️ Common Commands

### Build Commands
```bash
# Clean and compile
mvn clean compile

# Package JAR (skip tests)
mvn clean package -DskipTests

# Install to local Maven repo
mvn clean install

# Clean build directory
mvn clean
```

### Development Workflow
```bash
# Make code changes...

# Compile (fast feedback)
mvn compile

# Run tests
mvn test

# Run application
mvn spring-boot:run
```

---

## 🐛 Troubleshooting

### Issue: "Port 8080 already in use"
**Solution:**
```bash
# Windows - Find process on port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID)
taskkill /PID <PID> /F
```

### Issue: "Cannot connect to database"
**Check:**
1. MySQL is running
2. Database `finance_tracker` exists
3. Username/password correct in `application.properties`
4. Port 3306 is not blocked

**Verify connection:**
```bash
mysql -u root -p finance_tracker
```

### Issue: "Maven dependencies not downloading"
**Solution:**
```bash
# Force update dependencies
mvn clean install -U

# Clear Maven cache (if needed)
# Delete: C:\Users\<YourUser>\.m2\repository
```

### Issue: "Lombok not working in IDE"
**IntelliJ IDEA:**
1. File → Settings → Plugins
2. Install "Lombok" plugin
3. File → Settings → Build → Compiler → Annotation Processors
4. Enable "Enable annotation processing"

**Eclipse:**
1. Download lombok.jar
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation
4. Restart Eclipse

---

## 📊 Development Lifecycle

### Starting Fresh Development Session
```bash
# 1. Pull latest code (if using Git)
git pull origin master

# 2. Clean build
mvn clean install

# 3. Run tests
mvn test

# 4. Start application
mvn spring-boot:run
```

### Before Committing Code
```bash
# 1. Run tests
mvn test

# 2. Check for compilation errors
mvn clean compile

# 3. Format code (if checkstyle configured)
mvn checkstyle:check

# 4. Stage and commit
git add .
git commit -m "Your message"
git push
```

---

## 🔑 Environment Variables (Production)

For production, use environment variables instead of hardcoded values:

### Windows
```bash
# Set temporarily (current session)
set DATABASE_URL=jdbc:mysql://prod-host:3306/finance_tracker?useSSL=true&serverTimezone=UTC
set DATABASE_USERNAME=prod_user
set DATABASE_PASSWORD=prod_password
set JWT_SECRET=your-production-secret-key

# Run application
mvn spring-boot:run
```

### Linux/Mac
```bash
# Set temporarily
export DATABASE_URL=jdbc:mysql://prod-host:3306/finance_tracker?useSSL=true&serverTimezone=UTC
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=prod_password
export JWT_SECRET=your-production-secret-key

# Run application
mvn spring-boot:run
```

### Update application.properties
```properties
spring.datasource.url=${DATABASE_URL:jdbc:mysql://localhost:3306/finance_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DATABASE_USERNAME:root}
spring.datasource.password=${DATABASE_PASSWORD:root}
jwt.secret=${JWT_SECRET:dev-secret-key}
```

---

## 📱 Testing with Postman/cURL (Phase 3+)

### Example: Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Example: Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

Response will contain JWT token.

### Example: Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/transactions \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## 🎯 Quick Verification Checklist

Before each phase completion:

- [ ] Code compiles: `mvn compile` ✅
- [ ] Tests pass: `mvn test` ✅
- [ ] Application starts: `mvn spring-boot:run` ✅
- [ ] No errors in console ✅
- [ ] Database connection works (Phase 2+) ✅
- [ ] API endpoints respond (Phase 3+) ✅
- [ ] Swagger UI accessible (Phase 7) ✅

---

## 💡 Pro Tips

1. **Hot Reload**: Use Spring Boot DevTools for automatic restart on code changes
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-devtools</artifactId>
       <scope>runtime</scope>
       <optional>true</optional>
   </dependency>
   ```

2. **Profile-based Configuration**: Use different configurations for dev/prod
   - `application-dev.properties`
   - `application-prod.properties`
   - Run: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

3. **Logging**: Adjust logging levels in `application.properties`
   ```properties
   logging.level.com.finance.tracker=DEBUG
   logging.level.org.springframework.web=INFO
   ```

4. **Database GUI Tools**:
   - MySQL Workbench (MySQL official)
   - DBeaver (multi-database)
   - IntelliJ IDEA Database Tool
   - phpMyAdmin (web-based)

---
