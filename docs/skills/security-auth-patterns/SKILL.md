---
name: security-auth-patterns
description: Spring Security configuration, JWT authentication, role-based access control, and auth context patterns for the AI Donor Matcher backend. Use when modifying security rules, adding endpoints to SecurityConfig, extracting authenticated user info, or implementing auth-related features.
---

# Security & Auth Patterns

## Authentication Flow
```
1. POST /api/auth/register → User created → verification email sent
2. GET /api/auth/verify?token= → emailVerified = true
3. POST /api/auth/login → credentials validated → JWT returned
4. All subsequent requests → Authorization: Bearer <jwt> → JwtFilter validates
```

## JWT Structure
- Library: `io.jsonwebtoken:jjwt` 0.12.3
- Token contains: email (subject), issued-at, expiration
- Secret: `jwt.secret` in application.properties (base64-encoded 256-bit)
- Expiry: `jwt.expiration-ms` (default 86400000 = 24h)

## JwtFilter Chain
```
Request → JwtFilter → Extract "Bearer" token → Extract email
  → Load UserDetails → Validate token → Set SecurityContext → Continue
```
- Runs on every request via `OncePerRequestFilter`
- Skips if no `Authorization: Bearer` header present
- Sets `UsernamePasswordAuthenticationToken` in SecurityContext

## SecurityConfig — Endpoint Rules
```java
.authorizeHttpRequests(auth -> auth
    // Public
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
    // Role-restricted
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/ngo/**").hasRole("NGO")
    .requestMatchers("/api/pledges/**").hasRole("DONOR")
    // Everything else
    .anyRequest().authenticated()
)
```

### Adding New Endpoints
When adding a new endpoint that doesn't fit existing patterns, add a **specific** rule BEFORE `.anyRequest()`:
```java
// Example: POST /api/needs requires NGO role
.requestMatchers(HttpMethod.POST, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.PUT, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.DELETE, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.PATCH, "/api/needs/**").hasRole("NGO")
```

## Roles
```java
public enum Role {
    DONOR,   // Can pledge, view NGOs, report
    NGO,     // Can manage profile, needs
    ADMIN    // Can verify/suspend NGOs, moderate needs, view reports
}
```
Spring Security authority format: `ROLE_DONOR`, `ROLE_NGO`, `ROLE_ADMIN`

## User Entity as UserDetails
`User` implements `UserDetails`:
```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}

@Override
public String getUsername() { return email; }  // email is the principal
```

## Extracting Current User in Controllers
```java
@GetMapping("/profile")
public ResponseEntity<?> getProfile(Authentication auth) {
    String email = auth.getName();  // extracts email
    return ResponseEntity.ok(service.getProfile(email));
}
```
Or from SecurityContext in services (avoid — prefer passing email from controller):
```java
String email = SecurityContextHolder.getContext().getAuthentication().getName();
```

## CORS Configuration
```java
config.setAllowedOrigins(List.of(
    "https://your-vercel-frontend.vercel.app",
    "http://localhost:5173"
));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

## Password Handling
- BCrypt via `PasswordEncoder` bean
- Never store plaintext passwords
- Never return password in responses

## Error Handling for Auth
| Scenario | Exception | HTTP Status |
|----------|-----------|-------------|
| Wrong email/password | `BadCredentialsException` | 401 |
| Token expired/invalid | JWT library exception → 401 | 401 |
| Wrong role | Spring Security → `AccessDeniedException` | 403 |
| Unverified email | Business logic `RuntimeException` | 400 |

## Security Rules
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- CSRF disabled (JWT-based API)
- No session cookies
- All auth via `Authorization: Bearer <token>` header
