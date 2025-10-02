# 📚 PHASE 3 - JWT Authentication & Spring Security

## Complete Explanation for Learning & Interviews

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components Deep Dive](#components-deep-dive)
4. [Security Flow](#security-flow)
5. [Interview Questions & Answers](#interview-questions--answers)
6. [Common Pitfalls](#common-pitfalls)
7. [Best Practices](#best-practices)

---

## Overview

### What We Built

Phase 3 implements **stateless JWT-based authentication** using Spring Security. This allows users to:
- Register with email/password
- Login and receive a JWT token
- Access protected endpoints using the token
- Maintain security without server-side sessions

### Why JWT Over Sessions?

| Feature | Sessions | JWT |
|---------|----------|-----|
| **Storage** | Server-side | Client-side |
| **Scalability** | Requires sticky sessions | Horizontally scalable |
| **Microservices** | Complex | Simple |
| **Mobile Apps** | Challenging | Native support |
| **Performance** | DB/Redis lookup | Self-contained |

---

## Architecture

### Component Flow Diagram

```
┌──────────┐         ┌────────────────┐         ┌──────────┐
│          │         │                │         │          │
│  Client  │────────>│ SecurityConfig │────────>│   JWT    │
│          │         │                │         │  Filter  │
└──────────┘         └────────────────┘         └──────────┘
                              │                       │
                              v                       v
                     ┌────────────────┐      ┌───────────────┐
                     │                │      │               │
                     │  Auth Service  │<────>│ UserDetails   │
                     │                │      │   Service     │
                     └────────────────┘      └───────────────┘
                              │                       │
                              v                       v
                     ┌────────────────┐      ┌───────────────┐
                     │                │      │               │
                     │   Controller   │      │  Repository   │
                     │                │      │               │
                     └────────────────┘      └───────────────┘
```

---

## Components Deep Dive

### 1. JwtUtil.java

**Purpose:** Central JWT token management

#### Key Methods:

##### `generateToken(UserDetails userDetails)`
```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, userDetails.getUsername());
}
```

**What it does:**
1. Takes Spring Security's `UserDetails` (contains email, password, authorities)
2. Creates empty claims map (can add custom data)
3. Calls `createToken()` with email as subject

**Interview Tip:** Explain that "subject" in JWT is the primary identifier (in our case, user's email)

##### `createToken(Map<String, Object> claims, String subject)`
```java
private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .setClaims(claims)           // Custom data
        .setSubject(subject)         // User identifier (email)
        .setIssuedAt(new Date())     // Token creation time
        .setExpiration(new Date(...)) // Token expiry (24 hours)
        .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Sign with secret
        .compact();                   // Build final token string
}
```

**JWT Structure:**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
     HEADER            PAYLOAD (Base64)                   SIGNATURE
```

##### `validateToken(String token, UserDetails userDetails)`
```java
public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
}
```

**Validation Checks:**
1. Extract email from token
2. Compare with UserDetails email (ensure token belongs to this user)
3. Check if token hasn't expired

**Interview Question:** *"What happens if someone steals a JWT token?"*
- **Answer:** They can use it until expiration. This is why:
  - Keep expiration short (24 hours)
  - Use HTTPS only
  - Store securely (HttpOnly cookies, not localStorage)
  - Implement token refresh mechanism (Phase 6)

---

### 2. JwtAuthenticationFilter.java

**Purpose:** Intercepts EVERY request to validate JWT

#### Extends `OncePerRequestFilter`
- Guarantees the filter runs **exactly once** per request
- Even if request is forwarded/redirected internally

#### Filter Flow:

```java
protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain) {
    
    // 1. Extract Authorization header
    final String authHeader = request.getHeader("Authorization");
    
    // 2. Check if header exists and starts with "Bearer "
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response); // Continue without auth
        return;
    }
    
    // 3. Extract token (remove "Bearer " prefix)
    jwt = authHeader.substring(7);
    
    // 4. Extract email from token
    userEmail = jwtUtil.extractUsername(jwt);
    
    // 5. If email exists and user not already authenticated
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        
        // 6. Load user from database
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        
        // 7. Validate token
        if (jwtUtil.validateToken(jwt, userDetails)) {
            
            // 8. Create authentication object
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            
            // 9. Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    
    // 10. Continue filter chain
    filterChain.doFilter(request, response);
}
```

**Interview Deep Dive:**

**Q: "Why check if authentication is null?"**
- **A:** If user is already authenticated (maybe by another filter), skip re-authentication. Improves performance.

**Q: "What's SecurityContextHolder?"**
- **A:** Thread-local storage for security information. Each HTTP request has its own SecurityContext. Spring Security uses this to check if user is authenticated.

**Q: "What happens if token is invalid?"**
- **A:** Filter continues WITHOUT setting authentication. Later, when controller requires auth, Spring Security throws `AuthenticationException` → 401 Unauthorized.

---

### 3. UserDetailsServiceImpl.java

**Purpose:** Bridge between Spring Security and our database

```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    
    // 1. Fetch user from database by email
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    
    // 2. Convert our User entity to Spring Security's UserDetails
    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),        // username
        user.getPassword(),     // password (BCrypt hash)
        new ArrayList<>()       // authorities (roles) - empty for now
    );
}
```

**Why This Exists:**
- Spring Security doesn't know about YOUR database
- It only understands `UserDetails` interface
- This service translates: Your `User` → Spring's `UserDetails`

**Interview Scenario:**

**Interviewer:** *"Add role-based authorization (ADMIN, USER)"*

**Answer:**
```java
public UserDetails loadUserByUsername(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(...);
    
    // Create authorities from user roles
    List<GrantedAuthority> authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
        .collect(Collectors.toList());
    
    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        authorities  // Now with roles!
    );
}
```

---

### 4. SecurityConfig.java

**Purpose:** Configure Spring Security's behavior

#### Key Configurations:

##### 1. Security Filter Chain
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Disable CSRF (not needed for stateless JWT)
        .csrf(AbstractHttpConfigurer::disable)
        
        // Configure authorization
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // Public
            .anyRequest().authenticated()                 // Protected
        )
        
        // Stateless sessions (no JSESSIONID cookie)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // Add JWT filter BEFORE standard auth filter
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**Interview Questions:**

**Q: "Why disable CSRF for JWT?"**
- **A:** CSRF attacks exploit cookies auto-sent by browsers. JWT is manually added to headers, so not vulnerable to CSRF. However, vulnerable to XSS if token stored in localStorage.

**Q: "What's the filter order?"**
- **A:** 
  1. JwtAuthenticationFilter (our custom filter) - validates JWT
  2. UsernamePasswordAuthenticationFilter (Spring's) - handles form login (we don't use)
  3. Others...

##### 2. Authentication Provider
```java
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

**What it does:**
- Uses DAO (Database Access Object) pattern
- Loads user via `UserDetailsService`
- Compares passwords using `PasswordEncoder`

##### 3. Password Encoder
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**BCrypt Deep Dive:**

**How it works:**
```
Input: "password123"
Salt: automatically generated random string
Hash: BCrypt(password + salt) → "$2a$10$N9qo8..."
```

**Properties:**
- **Adaptive:** Can increase "work factor" as CPUs get faster
- **Salted:** Each password gets unique salt (prevents rainbow tables)
- **One-way:** Cannot decrypt, only verify
- **Slow by design:** ~100ms to hash (makes brute-force impractical)

**Interview Question:** *"How does password verification work?"*
```java
// Registration: Store hash
String hash = passwordEncoder.encode("password123"); 
// → "$2a$10$N9qo8..."

// Login: Verify
boolean matches = passwordEncoder.matches("password123", hash); 
// → true
```

BCrypt extracts salt from hash, re-hashes input, compares results.

---

### 5. DTOs (Data Transfer Objects)

#### Why DTOs?

**Problem:** Never expose entities directly to clients!
- Might leak sensitive data (password hash)
- Tight coupling between DB schema and API
- Can't customize response structure

#### LoginRequest.java
```java
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}
```

**Validations:**
- `@NotBlank`: Not null, not empty, not whitespace
- `@Email`: Valid email format (uses regex)

**Interview Tip:** Explain validation happens BEFORE controller method via `@Valid` annotation.

#### AuthResponse.java
```java
public class AuthResponse {
    private String token;       // JWT token
    private String email;
    private String firstName;
    private String lastName;
    private String message;     // Success message
}
```

**Never include:**
- ❌ Password (even hashed)
- ❌ Internal IDs (unless needed)
- ❌ Sensitive user data

---

### 6. AuthService.java

**Purpose:** Business logic for authentication

#### Registration Flow

```java
@Transactional
public AuthResponse register(RegisterRequest request) {
    
    // 1. Check if email exists
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new RuntimeException("Email already registered");
    }
    
    // 2. Create user entity
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash!
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    
    // 3. Save to database
    userRepository.save(user);
    
    // 4. Generate JWT token
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String jwtToken = jwtUtil.generateToken(userDetails);
    
    // 5. Return response
    return AuthResponse.builder()
        .token(jwtToken)
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .message("User registered successfully")
        .build();
}
```

**Why @Transactional?**
- If anything fails after `save()`, rollback database changes
- Ensures data consistency

**Interview Question:** *"What if email check passes, but another request registers same email before save()?"*
- **Answer:** Database unique constraint on email will throw exception. Transaction will rollback. This is why we have BOTH programmatic check AND database constraint.

#### Login Flow

```java
public AuthResponse login(LoginRequest request) {
    
    // 1. Authenticate credentials
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
    // If credentials invalid, throws BadCredentialsException
    
    // 2. Load user details
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
    
    // 3. Fetch user for additional info
    User user = userRepository.findByEmail(request.getEmail()).orElseThrow(...);
    
    // 4. Generate JWT token
    String jwtToken = jwtUtil.generateToken(userDetails);
    
    // 5. Return response
    return AuthResponse.builder()
        .token(jwtToken)
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .message("Login successful")
        .build();
}
```

**What AuthenticationManager does:**
1. Calls `UserDetailsService.loadUserByUsername(email)`
2. Gets hashed password from returned `UserDetails`
3. Uses `PasswordEncoder.matches(rawPassword, hashedPassword)`
4. If match → authenticated
5. If not → throws `BadCredentialsException`

---

### 7. AuthController.java

**Purpose:** REST API endpoints

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

**Annotations Explained:**

- `@RestController`: Combines `@Controller` + `@ResponseBody` (auto JSON conversion)
- `@RequestMapping("/api/auth")`: Base path for all methods
- `@RequiredArgsConstructor`: Lombok - generates constructor for `final` fields
- `@Valid`: Triggers validation on DTO fields
- `@RequestBody`: Deserialize JSON to Java object

**HTTP Status Codes:**
- `201 Created`: Registration success
- `200 OK`: Login success
- `400 Bad Request`: Validation failure (auto by Spring)
- `401 Unauthorized`: Invalid credentials (auto by Spring Security)

---

## Security Flow

### Complete Request Flow

#### Public Endpoint (e.g., /api/auth/register)

```
Client → HTTP POST /api/auth/register
         {email, password, firstName, lastName}
    ↓
SecurityFilterChain checks: permitAll() ✅
    ↓
@Valid validates RegisterRequest
    ↓
AuthController.register() called
    ↓
AuthService checks: email exists? ❌
    ↓
Password encrypted with BCrypt
    ↓
User saved to database
    ↓
JWT token generated
    ↓
Response: {token, email, firstName, lastName, message}
    ↓
Client stores token (localStorage/cookie)
```

#### Protected Endpoint (Future: /api/transactions)

```
Client → HTTP GET /api/transactions
         Header: Authorization: Bearer eyJhbG...
    ↓
JwtAuthenticationFilter intercepts
    ↓
Extract token from "Authorization" header
    ↓
Validate token signature & expiration
    ↓
Extract email from token
    ↓
Load user from database
    ↓
Set SecurityContext with user authentication
    ↓
SecurityFilterChain checks: authenticated() ✅
    ↓
TransactionController.getAllTransactions() called
    ↓
Return user's transactions
```

---

## Interview Questions & Answers

### Beginner Level

**Q1: What is JWT?**
- **A:** JSON Web Token - a compact, URL-safe token format for securely transmitting information between parties. Contains three parts: Header (algorithm), Payload (claims/data), Signature (verification).

**Q2: How is JWT different from session-based auth?**
- **A:** Sessions store user state on server (requires DB/Redis lookup). JWT is stateless - all info is in the token itself. Better for microservices and mobile apps.

**Q3: Why use BCrypt over MD5/SHA?**
- **A:** MD5/SHA are fast (bad for passwords - enables brute-force). BCrypt is intentionally slow with adaptive work factor and automatic salting.

### Intermediate Level

**Q4: Explain the JWT authentication flow**
- **A:** 
  1. User logs in with credentials
  2. Server validates, generates signed JWT token
  3. Client stores token, sends in Authorization header
  4. Server validates token signature on each request
  5. If valid, extracts user info and processes request

**Q5: What's in the SecurityContext?**
- **A:** Thread-local storage containing `Authentication` object with:
  - Principal (user details)
  - Credentials (usually null after auth)
  - Authorities (roles/permissions)
  - isAuthenticated flag

**Q6: How does @PreAuthorize work?**
- **A:** Spring Security annotation for method-level security. Example:
  ```java
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteUser(Long id) { ... }
  ```
  Checks SecurityContext authorities before method execution.

### Advanced Level

**Q7: How would you implement token refresh?**
- **A:** 
  1. Issue short-lived access token (15 min) + long-lived refresh token (7 days)
  2. Store refresh token in database with user association
  3. When access token expires, client sends refresh token
  4. Server validates refresh token, issues new access token
  5. Refresh token rotation: issue new refresh token, invalidate old

**Q8: How to handle token expiry gracefully?**
- **A:** 
  - Frontend: Axios interceptor catches 401, tries refresh, retries request
  - Backend: Custom AuthenticationEntryPoint returns JSON instead of redirect
  - Include expiry time in JWT response so client knows when to refresh

**Q9: Security concerns with JWT?**
- **A:** 
  - **XSS:** If stored in localStorage, vulnerable to XSS. Use HttpOnly cookies.
  - **Token theft:** Use HTTPS, short expiration, token refresh rotation
  - **Logout:** JWT can't be invalidated. Solution: token blacklist (Redis) or short expiry
  - **Secret key:** Must be strong (256-bit), stored securely (env vars, vault)

**Q10: How to implement role-based access control (RBAC)?**
- **A:** 
  ```java
  // 1. Add authorities to UserDetails
  List<GrantedAuthority> authorities = user.getRoles().stream()
      .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
      .collect(Collectors.toList());
  
  // 2. Configure SecurityConfig
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/admin/**").hasRole("ADMIN")
      .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
  )
  
  // 3. Method-level security
  @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(#id)")
  ```

---

## Common Pitfalls

### 1. Storing Password in Plain Text
❌ **Wrong:**
```java
user.setPassword(request.getPassword());
```

✅ **Correct:**
```java
user.setPassword(passwordEncoder.encode(request.getPassword()));
```

### 2. Returning Password in Response
❌ **Wrong:**
```java
return user; // Exposes password hash!
```

✅ **Correct:**
```java
return AuthResponse.builder()
    .email(user.getEmail())
    .firstName(user.getFirstName())
    .build(); // No password
```

### 3. Not Validating Token Expiration
❌ **Wrong:**
```java
String email = jwtUtil.extractUsername(token); // No expiry check
```

✅ **Correct:**
```java
if (jwtUtil.validateToken(token, userDetails)) { // Checks expiry
    // Proceed
}
```

### 4. Weak Secret Key
❌ **Wrong:**
```properties
jwt.secret=secret123
```

✅ **Correct:**
```properties
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
# 256-bit key
```

### 5. CSRF Protection for JWT
❌ **Wrong:**
```java
.csrf().enable() // Not needed for JWT
```

✅ **Correct:**
```java
.csrf(AbstractHttpConfigurer::disable) // Stateless JWT
```

---

## Best Practices

### 1. Token Management
- ✅ Keep expiration short (15 min - 24 hours)
- ✅ Use refresh tokens for long sessions
- ✅ Include token expiry in response
- ✅ Use strong signing algorithm (HS256/RS256)

### 2. Password Security
- ✅ Minimum length validation (6+ chars)
- ✅ Use BCrypt with work factor 10+
- ✅ Never log passwords
- ✅ Implement password reset flow

### 3. API Security
- ✅ Always use HTTPS in production
- ✅ Validate all inputs with `@Valid`
- ✅ Use DTOs, never expose entities
- ✅ Implement rate limiting (prevent brute-force)

### 4. Error Handling
- ✅ Generic error messages ("Invalid credentials" not "User not found")
- ✅ Custom exception handling (Phase 4)
- ✅ Don't leak implementation details

### 5. Code Organization
- ✅ Separate DTOs from entities
- ✅ Service layer for business logic
- ✅ Controller only handles HTTP
- ✅ Use @Transactional appropriately

---

## Testing Checklist

- [ ] User can register with valid data
- [ ] Registration fails with duplicate email
- [ ] Registration fails with invalid email format
- [ ] Password is hashed in database
- [ ] User can login with correct credentials
- [ ] Login fails with wrong password
- [ ] JWT token is returned on success
- [ ] Token contains correct email
- [ ] Token expires after configured time
- [ ] Protected endpoints require token
- [ ] Invalid token returns 401
- [ ] Expired token returns 401

---

## Summary

Phase 3 implemented:
- ✅ JWT token generation & validation
- ✅ BCrypt password encryption
- ✅ Spring Security configuration
- ✅ Stateless authentication
- ✅ User registration & login
- ✅ Security filter chain

**Key Takeaways for Interviews:**
1. Understand JWT structure (Header.Payload.Signature)
2. Explain stateless vs stateful authentication
3. Know BCrypt advantages over other algorithms
4. Describe Spring Security filter chain
5. Discuss security trade-offs and solutions

---



