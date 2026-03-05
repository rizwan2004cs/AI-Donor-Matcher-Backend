---
name: spring-boot-conventions
description: Coding conventions, architecture patterns, and project structure rules for the AI Donor Matcher Spring Boot backend. Use when writing any new Java class, modifying existing code, or reviewing code for consistency.
---

# Spring Boot Backend Conventions

## Architecture
Three-layer only: **Controller → Service → Repository**. No "manager" classes, no abstract base services, no generic interfaces wrapping services.

## Package Structure
```
com.aidonormatcher.backend/
├── config/          # Spring beans, security, Cloudinary
├── controller/      # REST endpoints (@RestController)
├── dto/             # Request/response records
├── entity/          # JPA entities (Lombok)
├── enums/           # Role, NgoStatus, NeedStatus, etc.
├── repository/      # JpaRepository interfaces
└── service/         # Business logic (@Service)
```

## Class Patterns

### Entity
```java
@Entity
@Table(name = "plural_name")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // fields...
}
```
- Use `@Builder.Default` when a field has a default initializer (e.g., `private int score = 0;`)
- Enum fields: `@Enumerated(EnumType.STRING)` always
- Timestamps: `LocalDateTime` for timestamps, `LocalDate` for dates

### DTO
```java
public record SomeRequest(
    @NotBlank String fieldName,
    @NotNull SomeEnum type,
    String optionalField
) {}
```
- Always Java `record` types, never classes
- Jakarta validation annotations on required fields
- No Lombok on DTOs — records handle immutability

### Service
```java
@Service
@RequiredArgsConstructor
public class SomeService {
    private final SomeRepository someRepository;
    private final OtherService otherService;
    // methods...
}
```
- All dependencies as `private final` fields — constructor injection via Lombok
- `@Transactional` on methods that do multiple writes
- Throw `RuntimeException("Descriptive message.")` for business errors
- Throw Spring's `BadCredentialsException` for auth failures

### Repository
```java
@Repository
public interface SomeRepository extends JpaRepository<Entity, Long> {
    Optional<Entity> findByField(String field);
    boolean existsByField(String field);
}
```
- Use Spring Data method naming for simple queries
- Use `@Query` with native SQL for complex queries (Haversine, etc.)

### Controller
```java
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class SomeController {
    private final SomeService someService;
    // endpoints...
}
```
- `@Valid` on `@RequestBody` parameters
- Extract current user email: `Authentication.getName()` from `SecurityContextHolder`
- Return `ResponseEntity<>` with appropriate HTTP status
- No business logic in controllers — delegate to service

## Naming Rules
| Element | Convention | Example |
|---------|-----------|---------|
| Class | PascalCase | `NeedService`, `AuthController` |
| Method/variable | camelCase | `findByEmail`, `trustScore` |
| REST endpoint | kebab-case | `/api/auth/resend-verification` |
| DB column | snake_case | `trust_score` (JPA default) |
| Enum values | UPPER_SNAKE | `PARTIALLY_PLEDGED` |

## Anti-Patterns (Do NOT)
- Create abstract/generic service base classes
- Add `@Slf4j` or logging unless explicitly requested
- Create utility classes for one-off operations
- Wrap services in interfaces
- Use `@Autowired` — always constructor injection
- Add comments on obvious code
