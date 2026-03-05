# AI Donor Matcher — Backend

## Project Overview
- **Framework:** Spring Boot 3.2.5, Java 21, Maven
- **Database:** PostgreSQL via Spring Data JPA
- **Security:** Spring Security + JWT (jjwt 0.12.3)
- **Package:** `com.aidonormatcher.backend`
- **Architecture:** Controller → Service → Repository (no intermediate "manager" layers)

## Brand & Design Identity
**Theme:** Warm & Humanitarian — Earth & Trust
| Token | Hex | Usage |
|-------|-----|-------|
| Primary | `#C1694F` | Terracotta — buttons, links, headings |
| Primary Dark | `#9E4A34` | Hover states, active elements |
| Secondary | `#3B6B4B` | Forest green — success, badges, accents |
| Secondary Dark | `#2A4D36` | Hover greens |
| Background | `#FAF7F2` | Warm cream page background |
| Surface | `#FFFFFF` | Cards, modals |
| Text Primary | `#2C2C2C` | Headings, body text |
| Text Secondary | `#6B6B6B` | Muted labels, descriptions |
| Border | `#E8E2D9` | Subtle warm borders |
| Warning | `#D4A843` | Amber warnings |
| Error | `#C14F4F` | Muted red errors |

## Coding Conventions

### Naming
- Classes: PascalCase (`NeedService`, `AuthController`)
- Methods/variables: camelCase (`findByEmail`, `trustScore`)
- Endpoints: kebab-case (`/api/auth/resend-verification`)
- Database columns: snake_case (JPA default mapping)

### Patterns This Project Uses
- **DTOs:** Java `record` types with Jakarta validation annotations
- **Entities:** `@Entity` + Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- **Services:** `@Service @RequiredArgsConstructor` with constructor-injected final fields
- **Repositories:** Extend `JpaRepository<Entity, Long>`, use `@Repository`
- **Error handling:** Throw `RuntimeException` (caught by GlobalExceptionHandler) or Spring's typed exceptions like `BadCredentialsException`
- **Auth context:** Extract email via `SecurityContextHolder` → `Authentication.getName()`
- **Transactions:** `@Transactional` on service methods that perform multiple writes

### What NOT to Do
- Do NOT create abstract base classes or generic service interfaces
- Do NOT add Swagger annotations until the Swagger skill is explicitly invoked
- Do NOT change existing DTOs from records to classes
- Do NOT add logging unless specifically requested
- Do NOT create helper/utility classes for single-use operations

## Project Status
- **Complete:** All 11 services, all entities, all repositories, all DTOs, all configs
- **Missing:** 6 controllers + GlobalExceptionHandler (see `docs/Feature_implementations/`)
- **Tests:** 69 unit tests across 11 service test files — ALL PASSING

## Build & Run
```bash
mvn clean compile   # compile
mvn test            # run all tests (69 tests)
mvn spring-boot:run # start app (needs DB + env vars)
```

## Skills
Project-specific skills are in `docs/skills/`. Each skill has a `SKILL.md` with detailed instructions:
- `spring-boot-conventions` — Architecture, class patterns, naming rules
- `controller-implementation` — All 6 missing controllers, endpoint specs, build order
- `testing-standards` — Mockito/AssertJ patterns, test naming, structure
- `api-design` — 27 endpoints, response formats, Swagger setup
- `database-entity-patterns` — Entity relationships, JPA patterns, repository queries
- `security-auth-patterns` — JWT flow, role-based access, SecurityConfig guide
