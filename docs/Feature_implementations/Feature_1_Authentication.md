# Feature 1 — Authentication (`AuthController`)

> **Priority:** Step 1 — build first, unblocks all other features  
> **Security:** All endpoints public (`/api/auth/**` is `.permitAll()`)  
> **Depends on:** `AuthService`, `JwtService`, `EmailService`  
> **New files:** 1 controller

---

## Endpoints

| Method | Path | Request Body | Response | Auth | Status |
|--------|------|-------------|----------|------|--------|
| `POST` | `/api/auth/register` | `RegisterRequest` | `200 "Registration successful..."` | No | 🔧 |
| `GET` | `/api/auth/verify?token={uuid}` | — | `200 "Email verified..."` | No | 🔧 |
| `POST` | `/api/auth/login` | `LoginRequest` | `200 LoginResponse` | No | 🔧 |
| `POST` | `/api/auth/resend-verification` | `{ email }` | `200 "Verification email resent."` | No | ⬜ |

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/AuthController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Accepts RegisterRequest { fullName, email, password, role, location? }
     * - role=DONOR → saves User, sends verification email
     * - role=NGO   → saves User + Ngo (status=PENDING, profileComplete=false), sends verification email
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Registration successful. Check your email to verify.");
    }

    /**
     * GET /api/auth/verify?token={uuid}
     *
     * Finds user by emailVerificationToken → sets emailVerified=true → clears token
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    /**
     * POST /api/auth/login
     *
     * Validates credentials via BCrypt, generates JWT.
     * Returns LoginResponse { token, userId, fullName, email, role, emailVerified }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── Feature 1.4 — Resend Verification (TODO: implement service method) ─

    // @PostMapping("/resend-verification")
    // public ResponseEntity<String> resendVerification(@RequestBody Map<String, String> body) {
    //     authService.resendVerification(body.get("email"));
    //     return ResponseEntity.ok("Verification email resent.");
    // }
}
```

---

## Actual Service Method Signatures

| Controller Method | Service Call | Signature |
|-------------------|-------------|-----------|
| `register()` | `authService.register(req)` | `void register(RegisterRequest req)` |
| `verifyEmail()` | `authService.verifyEmail(token)` | `void verifyEmail(String token)` |
| `login()` | `authService.login(req)` | `LoginResponse login(LoginRequest req)` |

---

## DTOs (already exist)

**`RegisterRequest`**
```java
public record RegisterRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotNull Role role,       // DONOR or NGO
    String location            // optional
) {}
```

**`LoginRequest`**
```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
```

**`LoginResponse`**
```java
public record LoginResponse(
    String token, Long userId, String fullName,
    String email, String role, boolean emailVerified
) {}
```

---

## Service Behaviour (from AuthService.java)

- **register:** Checks `existsByEmail` → encodes password → saves User → if NGO role, creates linked Ngo entity (status=PENDING, profileComplete=false, trustTier=NEW) → generates UUID token → calls `emailService.sendVerificationEmail(user, token)`
- **verifyEmail:** Finds by token → sets `emailVerified=true` → clears token → saves
- **login:** Finds by email → BCrypt match → generates JWT via `jwtService.generateToken(user)` → returns `LoginResponse(token, userId, fullName, email, role, emailVerified)`

---

## Error Handling (via GlobalExceptionHandler)

| Scenario | Exception | HTTP |
|----------|-----------|------|
| Duplicate email | `RuntimeException("Email already registered.")` | 400 |
| Invalid verify token | `RuntimeException("Invalid verification token.")` | 400 |
| Wrong email/password | `BadCredentialsException("Invalid email or password.")` | 401 |
| Missing required field | `MethodArgumentNotValidException` | 400 |

---

## Security Config (already in place)

```java
.requestMatchers("/api/auth/**").permitAll()
```

No changes needed.

---

## Feature 1.4 — Resend Verification Email (⬜ TODO)

**Not yet implemented in AuthService.** Needs a new service method:

```java
public void resendVerification(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found."));
    if (user.isEmailVerified()) {
        throw new RuntimeException("Email already verified.");
    }
    String token = UUID.randomUUID().toString();
    user.setEmailVerificationToken(token);
    userRepository.save(user);
    emailService.sendVerificationEmail(user, token);
}
```

Add this to AuthService and uncomment the controller endpoint when ready.

---

## Testing Checklist

- [ ] `POST /api/auth/register` role=DONOR → 200, user saved, email sent
- [ ] `POST /api/auth/register` role=NGO → 200, user + Ngo(PENDING) saved
- [ ] `POST /api/auth/register` duplicate email → 400
- [ ] `GET /api/auth/verify?token=valid` → 200, emailVerified=true
- [ ] `GET /api/auth/verify?token=bad` → 400
- [ ] `POST /api/auth/login` valid creds → 200, JWT in response
- [ ] `POST /api/auth/login` wrong password → 401
- [ ] `POST /api/auth/login` unknown email → 401
