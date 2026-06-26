# Design Document — Java Spring Boot 3 JWT Authentication Boilerplate

This document describes the architecture, patterns, and conventions of this project. It is intended to give contributors and AI assistants enough context to extend the codebase consistently.

---

## Architecture Overview

This is a **monolithic Spring Boot REST API** following a strict layered architecture:

```
HTTP Request
    └── SecurityFilterChain (JwtAuthFilter)
            └── Controller
                    └── Service
                            └── Repository
                                    └── PostgreSQL (JPA / Flyway)
```

Each layer has a single responsibility:
- **Controller:** Handles HTTP input/output, delegates all logic to services. Never calls repositories directly.
- **Service:** Contains all business logic. Orchestrates repositories and other services.
- **Repository:** Spring Data JPA interfaces. No business logic.
- **Entity:** JPA-mapped domain objects. Not exposed directly via HTTP.
- **DTO:** Shapes for request/response. Always used instead of exposing entities.

---

## Package Conventions

| Package | Purpose |
|---|---|
| `config/` | Spring `@Configuration` beans |
| `controller/` | `@RestController` classes — all HTTP endpoints live here |
| `dto/` | Request and response POJOs |
| `entity/` | JPA entities |
| `exception/` | Custom exceptions and global handler |
| `repository/` | Spring Data JPA repository interfaces |
| `security/` | JWT filter, token provider, entry point, user details service |
| `service/` | Business logic layer |

> **Convention:** All new controllers go in `controller/`. All new business logic goes in `service/`. Controllers must never call repositories directly.

---

## Authentication & Security

### JWT Flow

```
POST /auth/signup
  → AuthController
  → UserService.registerUser()     # BCrypt password, assign ROLE_USER, save user
  → AuthService.generateTokenForEmail()  # Generate JWT for new user
  → return SignUpResponse(token)

POST /auth/signin
  → AuthController
  → AuthService.authUser()         # AuthenticationManager validates credentials
  → JwtTokenProvider.generateToken()
  → return SignInResponse(token)

GET /user  (authenticated)
  → JwtAuthFilter validates Bearer token
  → SecurityContext populated
  → UserController
  → UserService.getAuthenticatedUser()
  → return UserResponse
```

### Token Details
- **Algorithm:** HS512 (HMAC SHA-512)
- **Claims:** `subject` (email), `issuedAt`, `expiration`
- **Transport:** `Authorization: Bearer <token>` request header
- **Expiry:** Configured via `jwt.expiration` in `application.yml` (milliseconds)

### Security Filter Chain
1. `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`
2. Extracts token from `Authorization` header
3. Validates token via `JwtTokenProvider.validateToken()`
4. Loads user via `CustomUserDetailsService.loadUserByUsername()`
5. Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
6. `JwtAuthEntryPoint` handles unauthenticated access → `401 Unauthorized`

### Password Encoding
`BCryptPasswordEncoder` — configured as a `@Bean` in `SecurityConfig`.

### UserDetails Loading
`CustomUserDetailsService.loadUserByUsername()` accepts either username or email via `UserRepository.findByUsernameOrEmail()`. Roles are mapped to `GrantedAuthority` via `SimpleGrantedAuthority`.

### Whitelisting Endpoints
Add public endpoints to `WHITE_LIST_URL` in `SecurityConfig.java`:
```java
private static final String[] WHITE_LIST_URL = {
    "/auth/**"
};
```

---

## Data Model

### User Entity
| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | PK, auto-generated |
| `name` | `String` | — |
| `username` | `String` | NOT NULL, UNIQUE |
| `email` | `String` | NOT NULL, UNIQUE |
| `password` | `String` | NOT NULL, BCrypt-encoded |
| `roles` | `Set<Role>` | ManyToMany, EAGER fetch |

> **Note:** During registration, `username` is set to the user's email address. This means `username` and `email` will always be identical for users registered via the API.

### Role Entity
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK, auto-generated |
| `name` | `String` | e.g. `ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR` |

- `Role` implements `GrantedAuthority`, returning `name` as the authority string.
- Roles are seeded at startup by `RoleInitializer` using `CommandLineRunner`.
- Default roles: `ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`.
- New users are assigned `ROLE_USER` on registration.

### Database Schema
Managed by **Flyway**. Migration files in `src/main/resources/db/migration/`.

Naming convention: `V{version}__{description}.sql`

`V1__init.sql` creates:
- `users` table
- `roles` table
- `users_roles` join table (ManyToMany, with cascade delete)

When adding new tables, create a new migration file (e.g., `V2__add_posts.sql`). Never modify existing migration files.

---

## DTO Design

All DTOs use Lombok:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

| DTO | Direction | Fields |
|---|---|---|
| `SignUpRequest` | Inbound | `name`, `email`, `password` |
| `SignUpResponse` | Outbound | `token` |
| `SignInRequest` | Inbound | `email`, `password` |
| `SignInResponse` | Outbound | `token` |
| `UserResponse` | Outbound | `id`, `name`, `username`, `email` |

> **Convention:** Never return JPA entities from controllers. Always map to a DTO in the service layer.

---

## Exception Handling

All exceptions are handled centrally in `GlobalExceptionHandler` (`@RestControllerAdvice`).

### Custom Exceptions

| Exception | When to throw |
|---|---|
| `ResourceNotFoundException` | Entity not found by ID, email, etc. |
| `DuplicateResourceException` | Unique constraint would be violated (e.g., duplicate email) |

Both support two constructors:
```java
new ResourceNotFoundException("User not found");
new ResourceNotFoundException("User", "email", email); // → "User not found with email: 'x@y.com'"
```

### Standard Error Response
```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with email: 'x@y.com'"
}
```

> **Convention:** Always throw typed custom exceptions from the service layer. Never construct error responses manually in controllers.

---

## Configuration Beans

### `SecurityConfig`
- Defines `SecurityFilterChain`, `PasswordEncoder`, `AuthenticationProvider`, `AuthenticationManager`
- Maintains `WHITE_LIST_URL` for public endpoints
- Wires `JwtAuthFilter` and `JwtAuthEntryPoint`

### `RoleInitializer`
- `CommandLineRunner` bean that seeds roles on startup
- Uses `existsByName()` to avoid duplicates
- To add new default roles, add them here

### `DomainConfig`
- `@EnableJpaRepositories` pointing to the `repository` package
- `@EnableTransactionManagement`
- Entity scanning is handled by `@SpringBootApplication` in `JavaSpringBootApplication`

### `JacksonConfig`
- Disables `FAIL_ON_UNKNOWN_PROPERTIES` (tolerant deserialization)
- Disables `ACCEPT_FLOAT_AS_INT`
- Disables `WRITE_DATES_AS_TIMESTAMPS`

---

## Testing Conventions

- **Framework:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- **Style:** Unit tests — controllers tested in isolation with mocked service dependencies
- **Location:** `src/test/java/` mirroring the source package structure

### Test Naming Convention
```
methodName_scenario_expectedOutcome()
```
Examples:
```java
getAuthenticatedUser_happyPath_returnsUserResponse()
getAuthenticatedUser_userNotFound_throwsResourceNotFoundException()
```

### What to Test
- Happy path returns correct HTTP status and response body
- Error cases throw the correct typed exception

---

## Extension Guide

### Adding a New Feature (e.g., Posts)

1. **Migration:** Add `src/main/resources/db/migration/V2__add_posts.sql`
2. **Entity:** Create `entity/Post.java` with JPA annotations
3. **Repository:** Create `repository/PostRepository.java` extending `JpaRepository`
4. **DTOs:** Create `dto/CreatePostRequest.java` and `dto/PostResponse.java`
5. **Service:** Create `service/PostService.java` with business logic
6. **Controller:** Create `controller/PostController.java` with `@RestController`
7. **Security:** Add any new public endpoints to `WHITE_LIST_URL` in `SecurityConfig`
8. **Tests:** Add `controller/PostControllerTest.java`

### Adding a New Role
Add the role name to `RoleInitializer.java`:
```java
addRoleIfNotFound("ROLE_NEW_ROLE", roleRepository);
```

### Adding Method-Level Security
`@EnableMethodSecurity` is already active in `SecurityConfig`. Use annotations on controller or service methods:
```java
@PreAuthorize("hasRole('ADMIN')")
```

---

## Known Decisions & Trade-offs

| Decision | Rationale |
|---|---|
| `username` is set to `email` on signup | Simplifies auth — only email is needed to log in. `username` field kept for potential future use. |
| `EAGER` fetch on `User.roles` | Roles are always needed for `GrantedAuthority` resolution. Acceptable for small role sets. |
| `CascadeType.ALL` on `User.roles` | Simplifies persistence but means deleting a user cascades to the join table. |
| No refresh token | Kept out of scope for the boilerplate. Add as a future enhancement if needed. |
