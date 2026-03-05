---
name: database-entity-patterns
description: JPA entity design, repository patterns, and database conventions used in the AI Donor Matcher backend. Use when creating new entities, writing custom queries, or understanding entity relationships.
---

# Database & Entity Patterns

## Entity Relationships
```
User (1) ←──── (1) Ngo
Ngo  (1) ←──── (*) Need
Need (1) ←──── (*) Pledge
User (1) ←──── (*) Pledge  (donor)
User (1) ←──── (*) Report  (reporter)
Ngo  (1) ←──── (*) Report  (reported NGO)
```

## Entity Template
```java
@Entity
@Table(name = "table_name")  // always plural snake_case
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ParentEntity parent;

    // Enums — always STRING
    @Enumerated(EnumType.STRING)
    private SomeEnum status;

    // Text fields with limits
    @Column(length = 2000)
    private String description;

    // Unique constraints
    @Column(unique = true, nullable = false)
    private String email;

    // Default values — use @Builder.Default
    @Builder.Default
    private int score = 0;

    @Builder.Default
    private boolean active = false;

    // Timestamps
    private LocalDateTime createdAt;

    // Computed/transient
    @Transient
    public int getDerived() {
        return someCalculation();
    }
}
```

## Existing Entities

### User
- Table: `users`
- Implements `UserDetails` (Spring Security)
- Fields: id, fullName, email (unique), password, role (enum), emailVerified, emailVerificationToken, location, createdAt
- Authority: `ROLE_` + role.name()

### Ngo
- Table: `ngos`
- `@OneToOne` with User (`user_id`)
- Fields: id, user, name, address, contactEmail, contactPhone, description, categoryOfWork, photoUrl, status, profileComplete, lat, lng, trustScore, trustTier, verifiedAt, lastActivityAt, rejectionReason, createdAt

### Need
- Table: `needs`
- `@ManyToOne` with Ngo (`ngo_id`)
- Fields: id, ngo, category, itemName, description, quantityRequired, quantityPledged, urgency, expiryDate, status, createdAt, fulfilledAt
- `@Transient getQuantityRemaining()` = required - pledged

### Pledge
- Table: `pledges`
- `@ManyToOne` with Need and User (donor)
- Fields: id, need, donor, quantity, status, createdAt, expiresAt, cancelledAt, fulfilledAt

### Report
- Table: `reports`
- `@ManyToOne` with User (reporter) and Ngo (reported)
- Fields: id, reporter, reportedNgo, reason, details, createdAt

## Repository Patterns

### Simple Queries — use method naming
```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
List<Ngo> findByStatus(NgoStatus status);
Optional<Ngo> findByUser(User user);
```

### Complex Queries — use @Query with native SQL
```java
@Query(value = """
    SELECT n.*, (6371 * acos(...)) AS distance_km
    FROM ngos n
    WHERE n.status = 'APPROVED'
      AND (:category IS NULL OR n.category_of_work = :category)
    HAVING distance_km <= :radius
    ORDER BY distance_km ASC
    """, nativeQuery = true)
List<Object[]> findNearby(@Param("lat") double lat, ...);
```

### Derived queries with multiple status filters
```java
Need findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
    Ngo ngo, List<NeedStatus> statuses);
```

## Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```
- `ddl-auto=update` — Hibernate manages schema (dev mode)
- PostgreSQL — production database
- All IDs are auto-generated `IDENTITY` (PostgreSQL serial)

## Rules
- No `@MappedSuperclass` or base entity classes
- No `@CreatedDate` / `@LastModifiedDate` auditing — timestamps set manually
- No `cascade` or `orphanRemoval` on relationships — manage explicitly in services
- No `@EntityGraph` — keep queries explicit
