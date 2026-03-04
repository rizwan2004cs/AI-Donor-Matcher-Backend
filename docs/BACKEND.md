# AI Donor Matcher — Backend Documentation

> **Repo:** `AI-Donor-Matcher-Backend`
> **Stack:** Java 17 · Spring Boot 3 · Spring Security · JWT · Spring Data JPA · PostgreSQL · JavaMail · Cloudinary
> **Deployed to:** Render (free tier)
> **Database:** Supabase or Neon (PostgreSQL, free tier)

---

## Table of Contents

1. [Boilerplate Setup](#1-boilerplate-setup)
2. [Project Structure](#2-project-structure)
3. [Environment Variables](#3-environment-variables)
4. [Database Schema](#4-database-schema)
5. [Security Configuration](#5-security-configuration)
6. [Feature Implementation Guide](#6-feature-implementation-guide)
   - [6.1 Registration & Email Verification](#61-registration--email-verification)
   - [6.2 Login & JWT](#62-login--jwt)
   - [6.3 NGO Verification Flow (Admin)](#63-ngo-verification-flow-admin)
   - [6.4 NGO Profile Completion & Go-Live Gate](#64-ngo-profile-completion--go-live-gate)
   - [6.5 NGO Needs Management](#65-ngo-needs-management)
   - [6.6 Location-Based NGO Discovery (Haversine Query)](#66-location-based-ngo-discovery-haversine-query)
   - [6.7 Pledge Creation (Split Pledging)](#67-pledge-creation-split-pledging)
   - [6.8 Pledge Auto-Expiry Scheduled Job](#68-pledge-auto-expiry-scheduled-job)
   - [6.9 Pledge Cancellation](#69-pledge-cancellation)
   - [6.10 Need Fulfillment & Trust Score](#610-need-fulfillment--trust-score)
   - [6.11 Trust Score Service](#611-trust-score-service)
   - [6.12 Email Service (JavaMail)](#612-email-service-javamail)
   - [6.13 Donor Report System](#613-donor-report-system)
   - [6.14 Admin Need Moderation](#614-admin-need-moderation)
   - [6.15 Suspension Cascade](#615-suspension-cascade)
   - [6.16 NGO Photo Upload (Cloudinary)](#616-ngo-photo-upload-cloudinary)
   - [6.17 Need Expiry Scheduled Job](#617-need-expiry-scheduled-job)
7. [REST API Reference](#7-rest-api-reference)
8. [Render Deployment](#8-render-deployment)

---

## 1. Boilerplate Setup

### 1.1 Generate the project

Go to [start.spring.io](https://start.spring.io) with these settings:

| Field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.2.x |
| Group | `com.aidonormatcher` |
| Artifact | `backend` |
| Packaging | Jar |
| Java | 17 |

**Dependencies to select:**
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Validation
- Java Mail Sender
- Spring Boot DevTools (development only)

Download, unzip, open in IntelliJ or VS Code.

### 1.2 Add JWT and Cloudinary dependencies manually

In `pom.xml` inside `<dependencies>`:

```xml
<!-- JWT -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.3</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>

<!-- Cloudinary -->
<dependency>
  <groupId>com.cloudinary</groupId>
  <artifactId>cloudinary-http44</artifactId>
  <version>1.36.0</version>
</dependency>
```

### 1.3 Enable Scheduling

Add `@EnableScheduling` to the main application class:

```java
@SpringBootApplication
@EnableScheduling
public class BackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
```

---

## 2. Project Structure

```
src/main/java/com/aidonormatcher/backend/
├── config/
│   ├── SecurityConfig.java       # Spring Security + CORS + endpoint rules
│   ├── JwtFilter.java            # JWT extraction + validation per request
│   ├── CloudinaryConfig.java     # Cloudinary bean
│   └── ApplicationConfig.java    # PasswordEncoder, AuthenticationManager beans
├── controller/
│   ├── AuthController.java
│   ├── NgoController.java
│   ├── NeedController.java
│   ├── PledgeController.java
│   ├── AdminController.java
│   └── ReportController.java
├── service/
│   ├── AuthService.java
│   ├── NgoService.java
│   ├── NeedService.java
│   ├── PledgeService.java
│   ├── TrustScoreService.java
│   ├── EmailService.java
│   ├── AdminService.java
│   ├── ScheduledJobService.java
│   └── CloudinaryService.java
├── repository/
│   ├── UserRepository.java
│   ├── NgoRepository.java
│   ├── NeedRepository.java
│   └── PledgeRepository.java
├── entity/
│   ├── User.java
│   ├── Ngo.java
│   ├── Need.java
│   ├── Pledge.java
│   └── Report.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── NgoDiscoveryDTO.java      # Returned by map discovery endpoint
│   ├── PledgeRequest.java
│   ├── PledgeResponse.java
│   └── NeedRequest.java
└── enums/
    ├── Role.java                 # DONOR, NGO, ADMIN
    ├── NgoStatus.java            # PENDING, APPROVED, SUSPENDED
    ├── NeedStatus.java           # OPEN, PARTIALLY_PLEDGED, FULLY_PLEDGED, FULFILLED, EXPIRED
    ├── NeedCategory.java         # FOOD, CLOTHING, MEDICINE, EDUCATION, HOUSEHOLD, OTHER
    ├── UrgencyLevel.java         # NORMAL, URGENT
    └── PledgeStatus.java         # ACTIVE, CANCELLED, EXPIRED, FULFILLED
```

---

## 3. Environment Variables

**Never hardcode secrets.** Use environment variables in Render's dashboard for production.

For local development, create `src/main/resources/application.properties`:

```properties
# Database (Supabase or Neon — use the connection string from the dashboard)
spring.datasource.url=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=your-256-bit-base64-encoded-secret-here
jwt.expiration-ms=86400000

# JavaMail (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-gmail-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Cloudinary
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# App
app.base-url=https://your-vercel-frontend.vercel.app
spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=2MB
```

> **Gmail App Password:** Go to Google Account → Security → 2-Step Verification → App Passwords. Generate one for "Mail". Use that as `spring.mail.password`, not your real Gmail password.

---

## 4. Database Schema

All tables are auto-created by Hibernate (`ddl-auto=update`). The entity definitions below define the schema.

### User entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;         // BCrypt hashed

    @Enumerated(EnumType.STRING)
    private Role role;               // DONOR, NGO, ADMIN

    private boolean emailVerified = false;
    private String emailVerificationToken;

    private String location;         // free text, used for display only
    private LocalDateTime createdAt;
}
```

### Ngo entity

```java
@Entity
@Table(name = "ngos")
public class Ngo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private NeedCategory categoryOfWork;

    private String photoUrl;         // Cloudinary URL

    @Enumerated(EnumType.STRING)
    private NgoStatus status;        // PENDING, APPROVED, SUSPENDED

    private boolean profileComplete = false;

    private Double lat;              // Geocoded from address
    private Double lng;

    private int trustScore = 0;

    @Enumerated(EnumType.STRING)
    private TrustTier trustTier;     // NEW, ESTABLISHED, TRUSTED

    private LocalDateTime verifiedAt;
    private LocalDateTime lastActivityAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
```

### Need entity

```java
@Entity
@Table(name = "needs")
public class Need {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Ngo ngo;

    @Enumerated(EnumType.STRING)
    private NeedCategory category;

    private String itemName;
    private String description;      // optional

    private int quantityRequired;
    private int quantityPledged = 0; // sum of all ACTIVE pledges

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;

    private LocalDate expiryDate;    // optional

    @Enumerated(EnumType.STRING)
    private NeedStatus status;       // OPEN, PARTIALLY_PLEDGED, FULLY_PLEDGED, FULFILLED, EXPIRED

    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;

    // Derived — not stored
    public int getQuantityRemaining() {
        return quantityRequired - quantityPledged;
    }
}
```

### Pledge entity

```java
@Entity
@Table(name = "pledges")
public class Pledge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User donor;

    @ManyToOne
    private Need need;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private PledgeStatus status;     // ACTIVE, CANCELLED, EXPIRED, FULFILLED

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // createdAt + 48 hours
}
```

### Report entity

```java
@Entity
@Table(name = "reports")
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User reporter;

    @ManyToOne
    private Ngo ngo;

    private String reason;           // FRAUD, INACTIVE, MISLEADING, OTHER
    private LocalDateTime reportedAt;
}
```

---

## 5. Security Configuration

### 5.1 SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
                // Role-restricted
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/ngo/**").hasRole("NGO")
                .requestMatchers("/api/pledges/**").hasRole("DONOR")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://your-vercel-frontend.vercel.app",
            "http://localhost:5173"   // local dev
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 5.2 JwtFilter.java

```java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        String email = jwtService.extractEmail(token);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (jwtService.isValid(token, user)) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

### 5.3 JwtService.java

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("role", user.getRole().name())
            .claim("userId", user.getId())
            .claim("emailVerified", user.isEmailVerified())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getKey())
            .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean isValid(String token, UserDetails user) {
        return extractEmail(token).equals(user.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
            .parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }
}
```

---

## 6. Feature Implementation Guide

### 6.1 Registration & Email Verification

**Endpoint:** `POST /api/auth/register`

**AuthService logic:**
1. Check if email already exists → throw exception if so.
2. Hash the password with BCrypt.
3. Save the new `User` entity with `emailVerified = false`.
4. If role is `NGO`: create a linked `Ngo` entity with `status = PENDING` and `profileComplete = false`. Save any uploaded credential documents (store file URL or path).
5. Generate a UUID token → store as `emailVerificationToken` on the User.
6. Call `EmailService.sendVerificationEmail(user, token)`.

**Email verification endpoint:** `GET /api/auth/verify?token={uuid}`
- Find user by token → set `emailVerified = true` → clear the token → save → return success.

```java
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
    authService.register(req);
    return ResponseEntity.ok("Registration successful. Check your email to verify.");
}

@GetMapping("/verify")
public ResponseEntity<?> verifyEmail(@RequestParam String token) {
    authService.verifyEmail(token);
    return ResponseEntity.ok("Email verified. You can now log in.");
}
```

---

### 6.2 Login & JWT

**Endpoint:** `POST /api/auth/login`

**AuthService logic:**
1. Load user by email. If not found → throw `BadCredentialsException`.
2. Compare raw password with BCrypt hash. If mismatch → throw `BadCredentialsException`.
3. Generate JWT via `JwtService.generateToken(user)`.
4. Return `LoginResponse` with the token and user details.

```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
    return ResponseEntity.ok(authService.login(req));
}
```

**LoginResponse DTO:**
```java
public record LoginResponse(
    String token,
    Long userId,
    String fullName,
    String email,
    String role,
    boolean emailVerified
) {}
```

---

### 6.3 NGO Verification Flow (Admin)

**Endpoints:**

```
GET  /api/admin/ngos/pending          → list all NGOs with status PENDING
POST /api/admin/ngos/{id}/approve     → set status = APPROVED, send approval email
POST /api/admin/ngos/{id}/reject      → set status = PENDING (allow reapply), save reason, send rejection email
```

**AdminService.approveNgo:**
```java
@Transactional
public void approveNgo(Long ngoId) {
    Ngo ngo = ngoRepository.findById(ngoId).orElseThrow();
    ngo.setStatus(NgoStatus.APPROVED);
    ngo.setVerifiedAt(LocalDateTime.now());
    ngoRepository.save(ngo);
    trustScoreService.recalculate(ngo); // verification = +40 pts
    emailService.sendNgoApprovedEmail(ngo);
}
```

**AdminService.rejectNgo:**
```java
@Transactional
public void rejectNgo(Long ngoId, String reason) {
    Ngo ngo = ngoRepository.findById(ngoId).orElseThrow();
    ngo.setRejectionReason(reason);
    ngoRepository.save(ngo);
    emailService.sendNgoRejectedEmail(ngo, reason);
}
```

---

### 6.4 NGO Profile Completion & Go-Live Gate

**Endpoints:**
```
GET  /api/ngo/my/profile              → return current NGO profile
PUT  /api/ngo/my/profile              → update profile fields
POST /api/ngo/my/photo                → upload photo to Cloudinary
```

**Profile completeness check** — called after every profile save:

```java
private boolean checkProfileComplete(Ngo ngo) {
    return ngo.getName() != null && !ngo.getName().isBlank()
        && ngo.getAddress() != null && !ngo.getAddress().isBlank()
        && ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()
        && ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()
        && ngo.getDescription() != null && ngo.getDescription().length() >= 50
        && ngo.getCategoryOfWork() != null;
}
```

**Go-live condition:** An NGO appears in the discovery query **only** when `status = APPROVED AND profileComplete = true`. This is enforced in the Haversine JPA query (see 6.6).

**Geocoding the address:** When the NGO saves their address, you need `lat` and `lng` to place the map pin. Use Nominatim (free, no key):

```java
// Call this after address is saved
public void geocodeAddress(Ngo ngo) {
    String encoded = URLEncoder.encode(ngo.getAddress(), StandardCharsets.UTF_8);
    String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
    RestTemplate rest = new RestTemplate();
    // Set User-Agent header (required by Nominatim)
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "AIDonorMatcher/1.0");
    ResponseEntity<List> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
    if (res.getBody() != null && !res.getBody().isEmpty()) {
        Map<?, ?> result = (Map<?, ?>) res.getBody().get(0);
        ngo.setLat(Double.parseDouble(result.get("lat").toString()));
        ngo.setLng(Double.parseDouble(result.get("lon").toString()));
        ngoRepository.save(ngo);
    }
}
```

---

### 6.5 NGO Needs Management

**Endpoints:**
```
GET    /api/ngo/my/needs              → list this NGO's needs
POST   /api/needs                     → create new need (NGO role)
PUT    /api/needs/{id}                → edit need (only if quantityPledged == 0)
DELETE /api/needs/{id}                → delete need (only if quantityPledged == 0)
PATCH  /api/needs/{id}/fulfill        → mark fulfilled (NGO role)
```

**NeedService.createNeed:**
```java
@Transactional
public Need createNeed(NeedRequest req, Long ngoId) {
    Ngo ngo = ngoRepository.findById(ngoId).orElseThrow();
    long activeCount = needRepository.countByNgoAndStatusIn(ngo,
        List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));
    if (activeCount >= 5) throw new RuntimeException("Maximum 5 active needs reached.");

    Need need = new Need();
    need.setNgo(ngo);
    need.setCategory(req.category());
    need.setItemName(req.itemName());
    need.setDescription(req.description());
    need.setQuantityRequired(req.quantityRequired());
    need.setUrgency(req.urgency());
    need.setExpiryDate(req.expiryDate());
    need.setStatus(NeedStatus.OPEN);
    need.setCreatedAt(LocalDateTime.now());
    return needRepository.save(need);
}
```

**Edit/Delete guard:** Before allowing edit or delete, check `need.getQuantityPledged() == 0`. If pledges exist, throw an error: _"This need is locked because it has active pledges."_

**Need status recalculation** — call after every pledge change:
```java
public void recalculateStatus(Need need) {
    int remaining = need.getQuantityRequired() - need.getQuantityPledged();
    if (need.getQuantityPledged() == 0) {
        need.setStatus(NeedStatus.OPEN);
    } else if (remaining > 0) {
        need.setStatus(NeedStatus.PARTIALLY_PLEDGED);
    } else {
        need.setStatus(NeedStatus.FULLY_PLEDGED);
    }
    needRepository.save(need);
}
```

---

### 6.6 Location-Based NGO Discovery (Haversine Query)

**Endpoint:** `GET /api/ngos?lat={}&lng={}&radius={}&category={}&search={}`

All params are optional. If `lat`/`lng` are not provided, skip radius filtering and return all live NGOs.

**NgoRepository — native Haversine query:**

```java
@Query(value = """
    SELECT n.*, 
    (6371 * acos(
        cos(radians(:lat)) * cos(radians(n.lat)) *
        cos(radians(n.lng) - radians(:lng)) +
        sin(radians(:lat)) * sin(radians(n.lat))
    )) AS distance_km
    FROM ngos n
    WHERE n.status = 'APPROVED'
      AND n.profile_complete = true
      AND (:category IS NULL OR n.category_of_work = :category)
      AND (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')))
    HAVING distance_km <= :radius
    ORDER BY distance_km ASC
    """, nativeQuery = true)
List<Object[]> findNearby(
    @Param("lat") double lat,
    @Param("lng") double lng,
    @Param("radius") double radius,
    @Param("category") String category,
    @Param("search") String search
);
```

**NgoService.discoverNgos — build NgoDiscoveryDTO:**

```java
public List<NgoDiscoveryDTO> discoverNgos(double lat, double lng,
                                           double radius, String category, String search) {
    List<Object[]> rows = ngoRepository.findNearby(lat, lng, radius, category, search);
    return rows.stream().map(row -> {
        Ngo ngo = (Ngo) row[0];
        double distanceKm = ((Number) row[1]).doubleValue();

        // Get the top active need (highest urgency first, then earliest created)
        Need topNeed = needRepository
            .findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
                ngo, List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));

        return new NgoDiscoveryDTO(
            ngo.getId(), ngo.getName(), distanceKm,
            ngo.getTrustScore(), ngo.getTrustTier().name(),
            topNeed != null ? topNeed.getItemName() : null,
            topNeed != null ? topNeed.getQuantityRemaining() : 0,
            topNeed != null ? topNeed.getUrgency().name() : null,
            topNeed != null ? topNeed.getCategory().name() : "OTHER",
            ngo.getLat(), ngo.getLng(), ngo.getPhotoUrl()
        );
    }).toList();
}
```

**Auto-expand (handled on frontend):** If the result is empty, the frontend re-calls the endpoint without a `radius` param. The backend should treat a missing `radius` as no radius filter (return all live NGOs).

---

### 6.7 Pledge Creation (Split Pledging)

**Endpoint:** `POST /api/pledges`

This is the most critical operation. It must be `@Transactional` with pessimistic locking to prevent two donors from over-pledging simultaneously.

```java
@Transactional
public PledgeResponse createPledge(PledgeRequest req, Long donorId) {
    // 1. Load donor — verify role and email verification
    User donor = userRepository.findById(donorId).orElseThrow();
    if (!donor.isEmailVerified()) throw new RuntimeException("Email not verified.");

    // 2. Load need with pessimistic write lock (blocks concurrent transactions)
    Need need = needRepository.findByIdWithLock(req.needId());

    // 3. Validate: need must be OPEN or PARTIALLY_PLEDGED
    if (need.getStatus() == NeedStatus.FULLY_PLEDGED
        || need.getStatus() == NeedStatus.FULFILLED
        || need.getStatus() == NeedStatus.EXPIRED) {
        throw new RuntimeException("This need is no longer available for pledging.");
    }

    // 4. Validate: requested quantity does not exceed remaining
    int remaining = need.getQuantityRemaining();
    if (req.quantity() > remaining) {
        throw new RuntimeException("Quantity requested exceeds remaining: " + remaining);
    }

    // 5. Create pledge
    Pledge pledge = new Pledge();
    pledge.setDonor(donor);
    pledge.setNeed(need);
    pledge.setQuantity(req.quantity());
    pledge.setStatus(PledgeStatus.ACTIVE);
    pledge.setCreatedAt(LocalDateTime.now());
    pledge.setExpiresAt(LocalDateTime.now().plusHours(48));
    pledgeRepository.save(pledge);

    // 6. Update need quantity and recalculate status
    need.setQuantityPledged(need.getQuantityPledged() + req.quantity());
    needService.recalculateStatus(need);

    // 7. Update NGO last activity
    Ngo ngo = need.getNgo();
    ngo.setLastActivityAt(LocalDateTime.now());
    ngoRepository.save(ngo);

    // 8. Send confirmation email
    emailService.sendPledgeConfirmationEmail(donor, pledge, ngo);

    // 9. Return response with NGO coordinates for OSRM navigation
    return new PledgeResponse(
        pledge.getId(), ngo.getLat(), ngo.getLng(),
        ngo.getAddress(), ngo.getContactEmail(), pledge.getExpiresAt()
    );
}
```

**NeedRepository — pessimistic lock query:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT n FROM Need n WHERE n.id = :id")
Need findByIdWithLock(@Param("id") Long id);
```

---

### 6.8 Pledge Auto-Expiry Scheduled Job

Runs every hour. Expires any ACTIVE pledge older than 48 hours.

```java
@Service
@RequiredArgsConstructor
public class ScheduledJobService {

    private final PledgeRepository pledgeRepository;
    private final NeedService needService;
    private final EmailService emailService;

    @Scheduled(fixedRate = 3_600_000) // every hour in milliseconds
    @Transactional
    public void expireOldPledges() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<Pledge> expired = pledgeRepository
            .findByStatusAndCreatedAtBefore(PledgeStatus.ACTIVE, cutoff);

        for (Pledge pledge : expired) {
            pledge.setStatus(PledgeStatus.EXPIRED);
            pledgeRepository.save(pledge);

            Need need = pledge.getNeed();
            need.setQuantityPledged(need.getQuantityPledged() - pledge.getQuantity());
            needService.recalculateStatus(need);

            emailService.sendPledgeExpiredEmail(pledge.getDonor(), pledge);
        }
    }
}
```

---

### 6.9 Pledge Cancellation

**Endpoint:** `DELETE /api/pledges/{pledgeId}`

Only the pledge's own donor can cancel. Only ACTIVE pledges can be cancelled.

```java
@Transactional
public void cancelPledge(Long pledgeId, Long donorId) {
    Pledge pledge = pledgeRepository.findById(pledgeId).orElseThrow();
    if (!pledge.getDonor().getId().equals(donorId))
        throw new RuntimeException("Unauthorized.");
    if (pledge.getStatus() != PledgeStatus.ACTIVE)
        throw new RuntimeException("Only active pledges can be cancelled.");

    pledge.setStatus(PledgeStatus.CANCELLED);
    pledgeRepository.save(pledge);

    Need need = pledge.getNeed();
    need.setQuantityPledged(need.getQuantityPledged() - pledge.getQuantity());
    needService.recalculateStatus(need);

    emailService.sendPledgeCancelledByDonorEmail(need.getNgo(), pledge);
}
```

---

### 6.10 Need Fulfillment & Trust Score

**Endpoint:** `PATCH /api/needs/{id}/fulfill`

Only the owning NGO can mark their own need as fulfilled.

```java
@Transactional
public void fulfillNeed(Long needId, Long ngoUserId) {
    Need need = needRepository.findById(needId).orElseThrow();
    if (!need.getNgo().getUser().getId().equals(ngoUserId))
        throw new RuntimeException("Unauthorized.");

    // Mark need fulfilled
    need.setStatus(NeedStatus.FULFILLED);
    need.setFulfilledAt(LocalDateTime.now());
    needRepository.save(need);

    // Mark all active pledges on this need as fulfilled
    List<Pledge> activePledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);
    for (Pledge pledge : activePledges) {
        pledge.setStatus(PledgeStatus.FULFILLED);
        pledgeRepository.save(pledge);
        emailService.sendFulfillmentThankYouEmail(pledge.getDonor(), pledge, need.getNgo());
    }

    // Recalculate trust score
    trustScoreService.recalculate(need.getNgo());
}
```

---

### 6.11 Trust Score Service

Four inputs. Rule-based. Recalculates and stores the result on the NGO entity.

```java
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final NgoRepository ngoRepository;
    private final NeedRepository needRepository;

    public void recalculate(Ngo ngo) {
        int score = 0;

        // 1. Admin verification: +40
        if (ngo.getStatus() == NgoStatus.APPROVED) score += 40;

        // 2. Profile completeness: 0–20 (based on 6 required fields)
        int filledFields = 0;
        if (ngo.getName() != null && !ngo.getName().isBlank()) filledFields++;
        if (ngo.getAddress() != null && !ngo.getAddress().isBlank()) filledFields++;
        if (ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()) filledFields++;
        if (ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()) filledFields++;
        if (ngo.getDescription() != null && ngo.getDescription().length() >= 50) filledFields++;
        if (ngo.getCategoryOfWork() != null) filledFields++;
        score += (int) ((filledFields / 6.0) * 20);

        // 3. Fulfilled donations: +2 per fulfillment, capped at 30
        long fulfilledCount = needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED);
        score += (int) Math.min(30, fulfilledCount * 2);

        // 4. Activity recency penalty: -10 if no activity for 60+ days
        if (ngo.getLastActivityAt() != null) {
            long daysSinceActivity = ChronoUnit.DAYS.between(
                ngo.getLastActivityAt(), LocalDateTime.now());
            if (daysSinceActivity > 60) score -= 10;
        }

        // Clamp to 0–100
        score = Math.max(0, Math.min(100, score));

        ngo.setTrustScore(score);
        ngo.setTrustTier(score >= 70 ? TrustTier.TRUSTED
                       : score >= 40 ? TrustTier.ESTABLISHED
                                     : TrustTier.NEW);
        ngoRepository.save(ngo);
    }
}
```

**When to call recalculate:**
- Admin approves NGO
- NGO updates profile
- NGO fulfills a need
- Scheduled daily job (to apply recency penalty)

---

### 6.12 Email Service (JavaMail)

All 12 triggers. Uses Gmail SMTP via Spring's `JavaMailSender`.

```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    public void sendVerificationEmail(User user, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        send(user.getEmail(), "Verify your AI Donor Matcher account",
            "Hi " + user.getFullName() + ",\n\nClick to verify:\n" + link);
    }

    public void sendNgoApprovedEmail(Ngo ngo) {
        send(ngo.getContactEmail(), "Your NGO application has been approved",
            "Congratulations! Complete your profile to go live on the map: "
            + baseUrl + "/dashboard/ngo/complete-profile");
    }

    public void sendNgoRejectedEmail(Ngo ngo, String reason) {
        send(ngo.getContactEmail(), "Your NGO application was not approved",
            "Reason: " + reason + "\n\nYou may reapply with corrected documents.");
    }

    public void sendPledgeConfirmationEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Pledge confirmed — " + pledge.getNeed().getItemName(),
            "Thank you! You pledged " + pledge.getQuantity() + " x " + pledge.getNeed().getItemName()
            + "\n\nDeliver to: " + ngo.getAddress()
            + "\nContact: " + ngo.getContactEmail()
            + "\nYour pledge expires in 48 hours.");
    }

    public void sendPledgeCancelledByDonorEmail(Ngo ngo, Pledge pledge) {
        send(ngo.getContactEmail(), "A pledge was cancelled",
            pledge.getDonor().getFullName() + " cancelled their pledge of "
            + pledge.getQuantity() + " x " + pledge.getNeed().getItemName() + ".");
    }

    public void sendPledgeExpiredEmail(User donor, Pledge pledge) {
        send(donor.getEmail(), "Your pledge has expired",
            "Your pledge of " + pledge.getQuantity() + " x "
            + pledge.getNeed().getItemName() + " has expired after 48 hours. "
            + "The item is still available — you can pledge again.");
    }

    public void sendFulfillmentThankYouEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Thank you for your donation!",
            "Your donation of " + pledge.getQuantity() + " x "
            + pledge.getNeed().getItemName() + " to " + ngo.getName()
            + " has been marked as received. Thank you!");
    }

    public void sendNgoApplicationAlert(User admin, Ngo ngo) {
        send(admin.getEmail(), "New NGO application: " + ngo.getName(),
            "A new NGO has applied. Review at: " + baseUrl + "/dashboard/admin");
    }

    public void sendNeedExpiryWarning(Ngo ngo, Need need) {
        send(ngo.getContactEmail(), "Need expiring soon: " + need.getItemName(),
            "Your need for " + need.getItemName() + " expires on " + need.getExpiryDate() + ".");
    }

    public void sendNeedAutoExpiredEmail(Ngo ngo, Need need) {
        send(ngo.getContactEmail(), "Need closed: " + need.getItemName(),
            "Your need for " + need.getItemName() + " has expired and been closed.");
    }

    public void sendReportReceivedEmail(User reporter) {
        send(reporter.getEmail(), "Report received",
            "Thank you. Your report has been received and will be reviewed by our team.");
    }

    public void sendAdminReportFlagEmail(User admin, Ngo ngo) {
        send(admin.getEmail(), "⚠️ NGO flagged: " + ngo.getName(),
            ngo.getName() + " has received 3 or more reports. Review at: "
            + baseUrl + "/dashboard/admin");
    }
}
```

---

### 6.13 Donor Report System

**Endpoints:**
```
POST /api/ngos/{id}/report            → donor submits report
GET  /api/admin/reports               → admin views report queue
```

**ReportService.submitReport:**
```java
@Transactional
public void submitReport(Long ngoId, String reason, Long reporterUserId) {
    User reporter = userRepository.findById(reporterUserId).orElseThrow();
    Ngo ngo = ngoRepository.findById(ngoId).orElseThrow();

    Report report = new Report();
    report.setReporter(reporter);
    report.setNgo(ngo);
    report.setReason(reason);
    report.setReportedAt(LocalDateTime.now());
    reportRepository.save(report);

    emailService.sendReportReceivedEmail(reporter);

    // Auto-flag if 3 or more reports
    long reportCount = reportRepository.countByNgo(ngo);
    if (reportCount >= 3) {
        User admin = userRepository.findByRole(Role.ADMIN).get(0);
        emailService.sendAdminReportFlagEmail(admin, ngo);
    }
}
```

---

### 6.14 Admin Need Moderation

**Endpoints:**
```
PUT    /api/admin/needs/{id}          → admin edits any NGO's need
DELETE /api/admin/needs/{id}          → admin removes a need (cascade)
```

**AdminService.removeNeed — cascade cancels pledges and notifies donors:**
```java
@Transactional
public void removeNeed(Long needId) {
    Need need = needRepository.findById(needId).orElseThrow();
    List<Pledge> activePledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);

    for (Pledge pledge : activePledges) {
        pledge.setStatus(PledgeStatus.CANCELLED);
        pledgeRepository.save(pledge);
        emailService.sendPledgeCancelledByDonorEmail(need.getNgo(), pledge); // reuse email
    }

    needRepository.delete(need);
}
```

---

### 6.15 Suspension Cascade

**Endpoint:** `POST /api/admin/ngos/{id}/suspend`

Single atomic `@Transactional` — all-or-nothing:

```java
@Transactional
public void suspendNgo(Long ngoId) {
    Ngo ngo = ngoRepository.findById(ngoId).orElseThrow();

    // 1. Set status to SUSPENDED (removes from donor map immediately)
    ngo.setStatus(NgoStatus.SUSPENDED);
    ngoRepository.save(ngo);

    // 2. Get all active needs for this NGO
    List<Need> activeNeeds = needRepository.findByNgoAndStatusIn(ngo,
        List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));

    for (Need need : activeNeeds) {
        // 3. Cancel all active pledges on each need
        List<Pledge> pledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);
        for (Pledge pledge : pledges) {
            pledge.setStatus(PledgeStatus.CANCELLED);
            pledgeRepository.save(pledge);
            // 4. Email each affected donor
            emailService.sendPledgeCancelledByDonorEmail(ngo, pledge);
        }
        // 5. Close the need
        need.setStatus(NeedStatus.EXPIRED);
        needRepository.save(need);
    }
    // If any step fails, the entire transaction rolls back. No partial state.
}
```

---

### 6.16 NGO Photo Upload (Cloudinary)

**CloudinaryConfig.java:**
```java
@Configuration
public class CloudinaryConfig {
    @Value("${cloudinary.cloud-name}") private String cloudName;
    @Value("${cloudinary.api-key}") private String apiKey;
    @Value("${cloudinary.api-secret}") private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
    }
}
```

**CloudinaryService.uploadPhoto:**
```java
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadPhoto(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap("folder", "ai-donor-matcher/ngos")
        );
        return result.get("secure_url").toString();
    }
}
```

**Controller usage:**
```java
@PostMapping("/api/ngo/my/photo")
public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile file,
                                      @AuthenticationPrincipal UserDetails userDetails) throws IOException {
    String url = cloudinaryService.uploadPhoto(file);
    ngoService.updatePhotoUrl(userDetails.getUsername(), url);
    return ResponseEntity.ok(Map.of("photoUrl", url));
}
```

---

### 6.17 Need Expiry Scheduled Job

Runs daily. Closes needs whose `expiryDate` has passed and warns NGOs 3 days before expiry.

```java
@Scheduled(cron = "0 0 0 * * *") // midnight every day
@Transactional
public void processNeedExpiry() {
    LocalDate today = LocalDate.now();
    LocalDate warningDate = today.plusDays(3);

    // Warn NGOs about needs expiring in 3 days
    List<Need> expiringWarnings = needRepository
        .findByExpiryDateAndStatusIn(warningDate,
            List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));
    for (Need need : expiringWarnings) {
        emailService.sendNeedExpiryWarning(need.getNgo(), need);
    }

    // Auto-close needs whose expiry date has passed
    List<Need> expired = needRepository
        .findByExpiryDateBeforeAndStatusIn(today,
            List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));
    for (Need need : expired) {
        // Cancel active pledges
        List<Pledge> activePledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);
        for (Pledge pledge : activePledges) {
            pledge.setStatus(PledgeStatus.CANCELLED);
            pledgeRepository.save(pledge);
        }
        need.setStatus(NeedStatus.EXPIRED);
        needRepository.save(need);
        emailService.sendNeedAutoExpiredEmail(need.getNgo(), need);
    }
}
```

---

## 7. REST API Reference

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register donor or NGO |
| GET | `/api/auth/verify` | Public | Verify email via token |
| POST | `/api/auth/login` | Public | Login, returns JWT |
| GET | `/api/ngos` | Public | Discover NGOs (Haversine) |
| GET | `/api/ngos/{id}` | Public | Get full NGO profile |
| POST | `/api/ngos/{id}/report` | DONOR | Report an NGO |
| GET | `/api/ngo/my/profile` | NGO | Get own profile |
| PUT | `/api/ngo/my/profile` | NGO | Update own profile |
| POST | `/api/ngo/my/photo` | NGO | Upload profile photo |
| GET | `/api/ngo/my/needs` | NGO | List own needs |
| GET | `/api/ngo/my/pledges` | NGO | Incoming pledge notifications |
| POST | `/api/needs` | NGO | Create a need |
| PUT | `/api/needs/{id}` | NGO | Edit a need |
| DELETE | `/api/needs/{id}` | NGO | Delete a need |
| PATCH | `/api/needs/{id}/fulfill` | NGO | Mark need as fulfilled |
| POST | `/api/pledges` | DONOR | Create a pledge |
| GET | `/api/pledges/{id}` | DONOR | Get pledge details |
| DELETE | `/api/pledges/{id}` | DONOR | Cancel a pledge |
| GET | `/api/pledges/my/active` | DONOR | Active pledges list |
| GET | `/api/pledges/my/history` | DONOR | Pledge history |
| GET | `/api/admin/stats` | ADMIN | Platform stats |
| GET | `/api/admin/ngos/pending` | ADMIN | Pending NGO queue |
| GET | `/api/admin/ngos` | ADMIN | All verified NGOs |
| POST | `/api/admin/ngos/{id}/approve` | ADMIN | Approve NGO |
| POST | `/api/admin/ngos/{id}/reject` | ADMIN | Reject NGO with reason |
| POST | `/api/admin/ngos/{id}/suspend` | ADMIN | Suspend NGO (cascade) |
| GET | `/api/admin/reports` | ADMIN | Report queue |
| PUT | `/api/admin/needs/{id}` | ADMIN | Edit any need |
| DELETE | `/api/admin/needs/{id}` | ADMIN | Remove any need (cascade) |

---

## 8. Render Deployment

1. Push repo to GitHub.
2. Go to [render.com](https://render.com) → New Web Service → Connect GitHub repo.
3. Settings:
   - **Environment:** Java
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/backend-0.0.1-SNAPSHOT.jar`
4. Add all environment variables from Section 3 in Render's "Environment" tab.
5. Free tier spins down after 15 minutes of inactivity. First request after idle takes ~30 seconds. This is expected for a prototype.
6. Use Render's Health Check URL: `/api/auth/health` — add a simple endpoint that returns `200 OK` so Render knows the app is up.

```java
@RestController
public class HealthController {
    @GetMapping("/api/auth/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```
