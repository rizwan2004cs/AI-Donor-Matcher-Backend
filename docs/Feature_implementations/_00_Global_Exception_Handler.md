# Step 0 — Global Exception Handler

> **Build this FIRST — all controllers depend on it.**

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/GlobalExceptionHandler.java`

```java
package com.aidonormatcher.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Catches all RuntimeExceptions thrown by service layer.
     * Examples: "Email already registered.", "NGO not found.", "Unauthorized."
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage(),
                "status", 400,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Catches BadCredentialsException from AuthService.login().
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Invalid email or password.",
                "status", 401,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Catches validation errors from @Valid on request DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", message,
                "status", 400,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

---

## Why This Is Needed

Without this handler, service exceptions like `RuntimeException("Email already registered.")` would return HTTP 500 with a Spring default error page. This maps them to proper HTTP status codes with JSON error bodies the frontend can parse.

| Exception | HTTP Status | When Thrown |
|-----------|-------------|-------------|
| `RuntimeException` | 400 Bad Request | Most service validation errors |
| `BadCredentialsException` | 401 Unauthorized | Login failure |
| `MethodArgumentNotValidException` | 400 Bad Request | `@Valid` DTO field violations |

---

## Response Format

All error responses follow this shape:

```json
{
  "error": "Email already registered.",
  "status": 400,
  "timestamp": "2026-03-05T10:30:00"
}
```

The frontend Axios interceptor can read `response.data.error` for user-facing messages.
