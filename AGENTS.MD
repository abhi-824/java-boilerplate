# AGENTS.md

## Project
Spring Boot REST API project
AI-assisted design + implementation with a real Postgres backend.

## Stack
- Java 21, Spring Boot 3.3.x, Maven
- PostgreSQL 16 (installed locally, not Docker)
- Spring Data JPA / Hibernate
- Spring Security + JWT (from boilerplate)
- JUnit 5 + Mockito for tests
- Flyway for migrations (no `ddl-auto: update` ever, even locally)

## Local Postgres setup
- Install Postgres locally (e.g. `brew install postgresql@16` on macOS, or
  the platform package manager equivalent) and start the service.
- Create a dedicated DB + user for this project rather than reusing the
  default `postgres` superuser:
  ```sql
  CREATE DATABASE app_db;
  CREATE USER app_user WITH PASSWORD 'changeme';
  GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
  ```
- Put connection details in `application-local.yml` (gitignored), not in
  the committed `application.yml`.
- Verify connection with `psql -U app_user -d app_db -h localhost` before
  pointing the app at it.

## Run / Build / Test
- Ensure local Postgres service is running (`pg_ctl status` or via your
  OS's service manager) before `mvn spring-boot:run`.
- Run app: `mvn spring-boot:run`
- Run tests: `mvn test`
- Run a single test class: `mvn test -Dtest=ExampleServiceTest`
- Migrations live in `src/main/resources/db/migration`, naming `V{n}__description.sql`

## Project structure
```
controller/   - REST endpoints, request/response DTOs only, no business logic
service/      - business logic, transactional boundaries
repository/   - Spring Data JPA interfaces, custom @Query methods
entity/       - JPA entities, no logic beyond simple invariants
dto/          - request/response shapes, never expose entities directly
config/       - security, JWT, swagger/openapi config
exception/    - custom exceptions + @ControllerAdvice handler
```

## Coding conventions
- DTOs at the controller boundary always — entities never cross into
  request/response bodies.
- Constructor injection only, no field `@Autowired`.
- Every service method that mutates data is `@Transactional`.
- Validation via `@Valid` + Bean Validation annotations on DTOs, not manual
  null checks in controllers.
- Enums for status fields (e.g. `ConnectionStatus`, `MessageStatus`), stored
  as `STRING` in Postgres, never ordinal.
- Exceptions: throw custom exceptions (`ResourceNotFoundException`, etc.),
  handled centrally in `@ControllerAdvice` — no try/catch sprawl in
  controllers.

## Commit discipline (important)
- One logical change per commit. Each commit must compile and pass tests
  on its own — no "WIP" or partially-broken commits.
- Suggested commit slicing for a new feature: migration → entity →
  repository → service (+ unit test) → controller (+ test) → wiring/docs.
- Commit message format: `<scope>: <what changed>`
  e.g. `contact: add Contact entity and repository`
- Update `docs/CHANGELOG.md` in the same commit as the change it describes.

## Documentation
- All docs live under `docs/` at repo root — not scattered in module folders.
- `docs/CHANGELOG.md` — one line per commit-worthy change, newest on top.
- `docs/DESIGN.md` — schema, API contract, auth rules (source of truth,
  written before implementation, not regenerated after the fact).
- Keep docs crisp: bullet points over prose, no restating obvious code.

## Testing expectations
- Every service method gets a unit test (Mockito for repository mocks).
- Every controller gets at least a happy-path + one auth-failure test.
- Don't skip tests "for now" — Aider should write the test in the same
  commit as the code it tests.

## Explicit don'ts
- Don't use `ddl-auto: update` or `create` anywhere, including local profile.
- Don't add a new dependency without flagging it first.
- Don't regenerate whole files when a small diff would do (prefer minimal,
  reviewable changes).
- Don't invent endpoints or fields not in `docs/DESIGN.md` — if something's
  missing from the design doc, stop and flag it rather than guessing.
- Don't touch files outside the current step's stated scope.

## Auth model (summary)
JWT-based, from boilerplate. Roles defined per project in `docs/DESIGN.md`
— don't assume a role model here; check the design doc first.