# Feature 1 — Authentication (`AuthController`)

> **Priority:** Step 1 — build first, unblocks all other features  
> **Security:** All endpoints public (`/api/auth/**` is `.permitAll()`)  
> **Depends on:** `AuthService`, `JwtService`, `EmailService`  
> **New files:** 1 controller

---

## Endpoints

| Method | Path | Request Body | Response | Auth |
|--------|------|-------------|----------|------|
| `POST` | `/api/auth/register` | `RegisterRequest` | `200 "Registration successful..."` | No |
| `GET` | `/api/auth/verify?token={uuid}` | — | `200 "Email verified..."` | No |
| `POST` | `/api/auth/login` | `LoginRequest` | `200 LoginResponse` | No |

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
     * - role=NGO   → saves User + Ngo (status=PENDING), sends verification email
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
}
```

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

## Service Method Mapping

| Controller Method | Service Call | Behaviour |
|-------------------|-------------|-----------|
| `register()` | `authService.register(req)` | Checks `existsByEmail`, encodes password, saves User, creates Ngo if NGO role, sends verification email |
| `verifyEmail()` | `authService.verifyEmail(token)` | Finds by token, sets `emailVerified=true`, clears token |
| `login()` | `authService.login(req)` | Finds by email, BCrypt match, generates JWT via `jwtService.generateToken()` |

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

## Testing Checklist

- [ ] `POST /api/auth/register` role=DONOR → 200, user saved, email sent
- [ ] `POST /api/auth/register` role=NGO → 200, user + Ngo(PENDING) saved
- [ ] `POST /api/auth/register` duplicate email → 400
- [ ] `GET /api/auth/verify?token=valid` → 200, emailVerified=true
- [ ] `GET /api/auth/verify?token=bad` → 400
- [ ] `POST /api/auth/login` valid creds → 200, JWT in response
- [ ] `POST /api/auth/login` wrong password → 401
- [ ] `POST /api/auth/login` unknown email → 401
