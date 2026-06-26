# LinkedIn Outreach App — Design Doc v1

## 1. DB Schema

```sql
-- ============================
-- PROFILE
-- ============================
CREATE TABLE profiles (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug              VARCHAR(255) NOT NULL UNIQUE,
    url               VARCHAR(512) NOT NULL,
    first_name        VARCHAR(255),
    last_name         VARCHAR(255),
    full_name         VARCHAR(255),
    email             VARCHAR(255),
    headline          VARCHAR(512),
    location          VARCHAR(255),
    about             TEXT,

    current_company   VARCHAR(255),   -- denormalized for fast filtering
    current_position   VARCHAR(255),

    scraped_at        TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),

    raw_payload       JSONB           -- full scrape dump, audit/fallback only, never queried
);

CREATE INDEX idx_profiles_current_company ON profiles(current_company);
CREATE INDEX idx_profiles_location ON profiles(location);

-- ============================
-- EXPERIENCE (1 profile -> many)
-- ============================
CREATE TABLE experiences (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profile_id        BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,

    title             VARCHAR(255),
    company           VARCHAR(255),
    employment_type   VARCHAR(100),
    location          VARCHAR(255),

    start_date        DATE,
    end_date          DATE,           -- null = "Present"
    is_current        BOOLEAN DEFAULT FALSE,
    dates_text        VARCHAR(255),   -- raw fallback, e.g. "Jan 2026 - Present"
    duration_text     VARCHAR(100),

    description       TEXT,
    sort_order         INT,

    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_experiences_profile_id ON experiences(profile_id);
CREATE INDEX idx_experiences_company ON experiences(company);
CREATE INDEX idx_experiences_title ON experiences(title);

-- ============================
-- EDUCATION (1 profile -> many)
-- ============================
CREATE TABLE education (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profile_id        BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,

    school            VARCHAR(255),
    degree            VARCHAR(255),
    field             VARCHAR(255),
    dates_text        VARCHAR(255),
    grade             VARCHAR(100),
    activities        TEXT,
    description       TEXT,
    sort_order         INT,

    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_education_profile_id ON education(profile_id);
CREATE INDEX idx_education_school ON education(school);

-- ============================
-- USERS / ROLES (existing boilerplate, shown for reference)
-- ============================
-- users(id, name, username, email, password)
-- roles(id, name)
-- users_roles(user_id, role_id)

-- ============================
-- USER <-> PROFILE MAP
-- ============================
CREATE TABLE user_profile_map (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id        BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,

    connected_on_text VARCHAR(50),    -- raw, e.g. "09 Jun 2026" — not parsed, not queried
    connection_status VARCHAR(50) NOT NULL DEFAULT 'CONNECTED', -- CONNECTED / PENDING / MESSAGED / REPLIED
    last_message_at   TIMESTAMP,

    created_at        TIMESTAMP NOT NULL DEFAULT now(),

    UNIQUE (user_id, profile_id)
);

CREATE INDEX idx_upm_user_id ON user_profile_map(user_id);
CREATE INDEX idx_upm_profile_id ON user_profile_map(profile_id);
CREATE INDEX idx_upm_status ON user_profile_map(connection_status);
```

**Notes**
- Skills, certifications, projects, recommendations are intentionally *not* modeled — left inside `raw_payload` since they won't be filtered on.
- `connected_on_text` stays a raw string per your call — no parsing/index needed.
- Filtering knobs this schema actually supports: company, title (current or historical, via `experiences`), school, location, connection status, per-user scoping.

---

## 2. API Contract

Base path: `/api`. All authenticated endpoints expect a Bearer JWT (issued at login).

### 2.1 Auth

Matches the existing boilerplate exactly — no new auth endpoints needed.

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| POST | `/auth/signup` | `{ name, email, password }` | `200 { token }` | `username` is set internally = email, per boilerplate |
| POST | `/auth/signin` | `{ email, password }` | `200 { token }` | |
| GET | `/user` | — (Bearer token) | `200 { id, name, username, email }` | already exists |

No `/auth/logout` — stateless JWT, client just discards the token (matches boilerplate, which has none).

### 2.2 Profiles

| Method | Path | Notes |
|---|---|---|
| GET | `/profiles` | List/search with filters (below) |
| GET | `/profiles/{id}` | Full profile incl. experience + education |
| POST | `/profiles` | Create/import a profile (used by scraper ingestion) |
| PUT | `/profiles/{id}` | Replace profile fields (re-scrape upsert) |
| DELETE | `/profiles/{id}` | Admin-only |

**GET `/profiles` query params** (all optional, combinable, AND-ed):

```
company        -> matches current_company OR any experiences.company (contains, case-insensitive)
title          -> matches current_position OR any experiences.title (contains)
school         -> matches education.school (contains)
location       -> matches profiles.location (contains)
connectedUser  -> userId; restricts to profiles connected to this user
status         -> connection_status (requires connectedUser)
page, size     -> pagination, default page=0, size=20
sort           -> e.g. "scrapedAt,desc"
```

Example: `GET /profiles?company=CRED&title=Android&connectedUser=12&status=CONNECTED&page=0&size=20`

Response:
```json
{
  "content": [
    {
      "id": 101,
      "slug": "batradheeraj20",
      "fullName": "Dheeraj Batra",
      "currentCompany": "Aspora",
      "currentPosition": "Android Developer",
      "location": "Bengaluru, Karnataka, India"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**GET `/profiles/{id}`** returns full nested detail:
```json
{
  "id": 101,
  "slug": "batradheeraj20",
  "url": "https://www.linkedin.com/in/batradheeraj20",
  "fullName": "Dheeraj Batra",
  "headline": "",
  "location": "",
  "about": "",
  "experience": [
    { "title": "Android Developer", "company": "Aspora", "datesText": "Jan 2026 - Present", "isCurrent": true }
  ],
  "education": [
    { "school": "Netaji Subhas Institute of Technology", "degree": "Bachelor of Engineering", "field": "Electrical, Electronics and Communications Engineering" }
  ]
}
```

### 2.3 Connections (User ↔ Profile)

| Method | Path | Notes |
|---|---|---|
| GET | `/users/{userId}/connections` | Filterable list, same filter params as `/profiles` plus `status` |
| POST | `/users/{userId}/connections` | `{ profileId, connectedOnText, status }` — link an existing profile to a user |
| PATCH | `/users/{userId}/connections/{profileId}` | `{ status, lastMessageAt }` — update outreach state |
| DELETE | `/users/{userId}/connections/{profileId}` | Unlink |

All endpoints return errors via the boilerplate's existing `GlobalExceptionHandler` (`@RestControllerAdvice`):
```json
{
  "timestamp": "2026-06-26T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Minimum username length: 4 characters"
}
```

New domain exceptions follow the existing pattern (`ResourceNotFoundException` → 404, `DuplicateResourceException` → 409):
- `ProfileNotFoundException` → 404 (extends `ResourceNotFoundException` or reuses it directly)
- `DuplicateResourceException` reused as-is for re-importing a profile with an existing `slug`
- `ConnectionAlreadyExistsException` → 409, for duplicate `(userId, profileId)` link attempts

---

## 3. Class Diagram (SOLID-oriented, following the boilerplate's package structure)

New code slots into the existing layout — no new top-level packages, just new files inside the existing ones:

```
src/main/java/com/abdude/java_spring_boot/
├── config/
│   ├── DomainConfig.java
│   ├── JacksonConfig.java
│   ├── RoleInitializer.java
│   └── SecurityConfig.java                 # add /profiles/** read-only rules if needed
├── controller/
│   ├── AuthController.java                 # existing, unchanged
│   ├── HomeController.java                 # existing, unchanged
│   ├── UserController.java                 # existing, unchanged
│   ├── ProfileController.java               # NEW
│   └── ConnectionController.java             # NEW
├── dto/
│   ├── SignInRequest.java / SignInResponse.java     # existing
│   ├── SignUpRequest.java / SignUpResponse.java     # existing
│   ├── UserResponse.java                            # existing
│   ├── ProfileSummaryDTO.java                       # NEW — list view
│   ├── ProfileDetailDTO.java                        # NEW — single profile + nested lists
│   ├── ExperienceDTO.java                            # NEW
│   ├── EducationDTO.java                              # NEW
│   ├── ProfileFilterRequest.java                       # NEW — company/title/school/location/page/size/sort
│   ├── ConnectionDTO.java                               # NEW
│   └── ConnectionUpdateRequest.java                       # NEW
├── entity/
│   ├── Role.java                            # existing
│   ├── User.java                            # existing
│   ├── Profile.java                          # NEW
│   ├── Experience.java                       # NEW
│   ├── Education.java                        # NEW
│   └── UserProfileMap.java                    # NEW
├── exception/
│   ├── DuplicateResourceException.java       # existing — reused for slug conflicts
│   ├── ResourceNotFoundException.java         # existing — reused for profile/connection 404s
│   └── GlobalExceptionHandler.java             # existing — extend with new exception mappings only if a new exception type is introduced (e.g. ConnectionAlreadyExistsException)
├── repository/
│   ├── RoleRepository.java                   # existing
│   ├── UserRepository.java                    # existing
│   ├── ProfileRepository.java                  # NEW — extends JpaRepository<Profile, Long>, JpaSpecificationExecutor<Profile>
│   ├── ExperienceRepository.java                # NEW
│   ├── EducationRepository.java                  # NEW
│   └── UserProfileMapRepository.java               # NEW
├── security/
│   ├── CustomUserDetailsService.java          # existing, unchanged
│   └── jwt/
│       ├── JwtAuthEntryPoint.java              # existing, unchanged
│       ├── JwtAuthFilter.java                   # existing, unchanged
│       └── JwtTokenProvider.java                 # existing, unchanged
├── service/
│   ├── AuthService.java                       # existing, unchanged
│   ├── UserService.java                        # existing, unchanged
│   ├── ProfileService.java                      # NEW interface
│   ├── ProfileServiceImpl.java                   # NEW
│   ├── ConnectionService.java                     # NEW interface
│   ├── ConnectionServiceImpl.java                  # NEW
│   └── spec/
│       └── ProfileSpecifications.java               # NEW — static Specification<Profile> builders
└── mapper/                                       # NEW package — small, single-purpose mappers (ISP)
    ├── ProfileMapper.java                          # Profile <-> ProfileSummaryDTO / ProfileDetailDTO
    ├── ExperienceMapper.java                        # Experience <-> ExperienceDTO
    ├── EducationMapper.java                          # Education <-> EducationDTO
    └── ConnectionMapper.java                           # UserProfileMap <-> ConnectionDTO
```

```
Entity relationships:
  User 1───* UserProfileMap *───1 Profile
  Profile 1───* Experience
  Profile 1───* Education
  User *───* Role  (existing, unchanged)

Dependency direction (DIP):
  ProfileController ──depends on──> ProfileService (interface)
  ProfileServiceImpl ──depends on──> ProfileRepository (interface), ProfileMapper
  ConnectionController ──depends on──> ConnectionService (interface)
  ConnectionServiceImpl ──depends on──> UserProfileMapRepository, ProfileRepository, ConnectionMapper
```

**SOLID notes**

- **SRP** — `ProfileController`/`ConnectionController` only translate HTTP↔DTO, same as the existing `UserController`/`AuthController`. Filtering logic lives entirely in `ProfileSpecifications`, not in the controller or repository.
- **OCP** — new filters (e.g. "connected after X") are added as new static `Specification<Profile>` methods in `ProfileSpecifications` and combined in `ProfileServiceImpl`; no existing class is modified to add a filter.
- **LSP** — `ProfileServiceImpl` and `ConnectionServiceImpl` are fully substitutable wherever their interfaces are injected (e.g. swap a fake in controller unit tests, same pattern the boilerplate already uses for `AuthService`/`UserService`).
- **ISP** — one mapper interface per entity rather than a single fat mapper; `ConnectionController` only depends on `ConnectionMapper`, not on `ProfileMapper`'s full surface.
- **DIP** — controllers and services depend on interfaces (`ProfileService`, `ConnectionService`, repository interfaces); Spring wires the concrete impls, exactly as `AuthController` already depends on `AuthService` rather than `AuthServiceImpl`.

## 4. Exception additions (only if needed)

Reuse existing `ResourceNotFoundException` / `DuplicateResourceException` wherever they fit (profile not found, duplicate slug on import). Only introduce a new exception class if a case doesn't map cleanly — e.g. `ConnectionAlreadyExistsException` for a duplicate `(userId, profileId)` link, mapped to `409` in `GlobalExceptionHandler` alongside the existing `DuplicateResourceException` entry.

---

## Open items for next pass
1. Whether `ProfileFilterRequest` should use `@RequestParam` flat params or a single `@ModelAttribute` filter object — affects controller signature only, not the contract above.
2. Pagination default sort field — suggest `scrapedAt,desc` unless you want `id,asc`.
3. ~~Auth: JWT vs session~~ — resolved: boilerplate is stateless JWT, no sessions table, no logout endpoint. Schema/contract above already reflect this.