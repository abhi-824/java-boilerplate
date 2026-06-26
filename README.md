# Java Spring Boot 3 JWT Authentication Maven Boilerplate

A clean, minimal Spring Boot 3 boilerplate featuring JWT authentication, role-based access control, and PostgreSQL integration. Designed as a solid starting point for secure Java REST APIs.

## Quick Start
- Configure Lombok on your IDE (IntelliJ: install Lombok plugin and enable annotation processing)
- `mvn clean install`
- `mvn clean package` or build from IDE
- `mvn spring-boot:run`

After starting, the application is accessible at `localhost:8080`.

---

## Project Structure

```
src/main/java/com/abdude/java_spring_boot/
├── config/
│   ├── DomainConfig.java              # JPA repository config and transaction management
│   ├── JacksonConfig.java             # Jackson ObjectMapper customization
│   ├── RoleInitializer.java           # Seeds default roles on startup
│   └── SecurityConfig.java            # Spring Security filter chain and JWT wiring
├── controller/
│   ├── AuthController.java            # POST /auth/signup, POST /auth/signin
│   ├── HomeController.java            # GET /
│   └── UserController.java            # GET /user (authenticated)
├── dto/
│   ├── SignInRequest.java
│   ├── SignInResponse.java
│   ├── SignUpRequest.java
│   ├── SignUpResponse.java
│   └── UserResponse.java
├── entity/
│   ├── Role.java                      # Implements GrantedAuthority
│   └── User.java                      # User entity with ManyToMany roles
├── exception/
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java    # Centralized @RestControllerAdvice
│   └── ResourceNotFoundException.java
├── repository/
│   ├── RoleRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java  # Loads user by username or email
│   └── jwt/
│       ├── JwtAuthEntryPoint.java     # Returns 401 on unauthorized access
│       ├── JwtAuthFilter.java         # Validates JWT on every request
│       └── JwtTokenProvider.java      # Generates and validates JWT tokens
└── service/
    ├── AuthService.java               # Sign-in and token generation logic
    └── UserService.java               # User registration and lookup logic
```

---

## Features
- **JWT Authentication:** Stateless, token-based auth using HS512-signed JWTs
- **Role-Based Access Control:** Roles (`ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`) seeded at startup
- **PostgreSQL + Flyway:** Schema managed via versioned SQL migrations
- **Global Exception Handling:** Consistent error response structure via `GlobalExceptionHandler`
- **Lombok:** Reduces boilerplate across DTOs and entities
- **Spring Security:** Custom JWT filter chain, stateless session management

---

## Tech Stack
- Java 21
- Spring Boot 3.3.5
- Maven
- Lombok
- PostgreSQL
- Flyway (database migrations)
- JJWT 0.12.3 (JWT generation and validation)
- Spring Security

---

## Configuration

Update `src/main/resources/application.yml` with your database and JWT settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db_name
    username: your_db_user
    password: your_db_password

jwt:
  secret: your_jwt_secret_key   # Must be HS512-compatible (Base64-encoded)
  expiration: 3600000            # Token expiry in milliseconds (1 hour)
```

Public (whitelisted) endpoints are configured in `SecurityConfig.java` via the `WHITE_LIST_URL` array.

---

## API Endpoints

### `POST /auth/signup`
Registers a new user. Returns a JWT token.

**Request body:**
```json
{
  "name": "Satoshi Nakamoto",
  "email": "satoshi.nakamoto@gmail.com",
  "password": "mysecretpw"
}
```

**Response:**
```json
{
  "token": "<jwt_token>"
}
```

> Note: `username` is automatically set to the user's email address internally.

---

### `POST /auth/signin`
Authenticates a user. Returns a JWT token.

**Request body:**
```json
{
  "email": "satoshi.nakamoto@gmail.com",
  "password": "mysecretpw"
}
```

**Response:**
```json
{
  "token": "<jwt_token>"
}
```

---

### `GET /user`
Returns the authenticated user's profile. Requires a valid JWT.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "id": 1,
  "name": "Satoshi Nakamoto",
  "username": "satoshi.nakamoto@gmail.com",
  "email": "satoshi.nakamoto@gmail.com"
}
```

---

### `GET /`
Public index endpoint. Returns `"Hello World!"`.

---

## JWT Authentication Flow

1. **Signup** — `POST /auth/signup` → `UserService.registerUser()` BCrypt-encodes the password, assigns `ROLE_USER`, saves the user, then `AuthService.generateTokenForEmail()` returns a JWT immediately.
2. **Signin** — `POST /auth/signin` → `AuthService.authUser()` uses Spring's `AuthenticationManager` to validate credentials, then `JwtTokenProvider.generateToken()` returns a signed JWT.
3. **Authenticated Requests** — `JwtAuthFilter` intercepts every request, extracts the `Bearer` token from the `Authorization` header, validates it via `JwtTokenProvider`, loads the user via `CustomUserDetailsService`, and sets the `Authentication` in the `SecurityContext`.
4. **Unauthorized Access** — `JwtAuthEntryPoint` returns `401 Unauthorized`.

---

## Security Configuration

`SecurityConfig` configures:
- **Disabled:** HTTP Basic auth, CSRF (stateless REST API)
- **Session Policy:** `STATELESS`
- **Whitelisted endpoints:** `/auth/**` (defined in `WHITE_LIST_URL`)
- **Authentication Provider:** `DaoAuthenticationProvider` with `CustomUserDetailsService` and `BCryptPasswordEncoder`
- **JWT Filter:** `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`

To whitelist additional endpoints, add them to `WHITE_LIST_URL` in `SecurityConfig.java`.

---

## Database

Schema is managed by Flyway. Migrations live in `src/main/resources/db/migration/`.

**`V1__init.sql`** creates:
- `users` table
- `roles` table
- `users_roles` join table (ManyToMany)

Roles (`ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`) are seeded at startup by `RoleInitializer` if they do not already exist.

---

## Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) handles:

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | `404 Not Found` |
| `DuplicateResourceException` | `409 Conflict` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `BadCredentialsException` | `401 Unauthorized` |
| `AuthenticationException` | `401 Unauthorized` |
| `IllegalArgumentException` | `400 Bad Request` |
| `Exception` (catch-all) | `500 Internal Server Error` |

All error responses follow this structure:
```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

---

## Development

Use the `local` Spring profile during development. In IntelliJ, add `-Dspring.profiles.active=local` to VM options in the Run Configuration. Create `src/main/resources/application-local.yml` to override settings locally.

---

## Build

```bash
mvn clean package
```

Run with a specific profile:

```bash
java -Dspring.profiles.active=production -jar ./target/java-spring-boot-0.0.1-SNAPSHOT.jar
```

Build a Docker image:

```bash
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=com.abdude/java-spring-boot
```

Set `SPRING_PROFILES_ACTIVE=production` as an environment variable when running the container.

---

## Testing

Tests use JUnit 5 with Mockito (`@ExtendWith(MockitoExtension.class)`). Controllers are unit tested in isolation with mocked service dependencies.

Test files mirror the source package structure under `src/test/java/`.

---

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.

---

## Support the Dev
- BTC: `bc1qpftdtjggrq8dpa6x6x7dnqvgv7ttc2x2m8rgvy`
- ETH / POL / BNB: `0xdCC9f5281B8bb40B11A792C280aA2cdd434C34AF`
