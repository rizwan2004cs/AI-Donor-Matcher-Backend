# Feature 8 — Report System (`ReportController`)

> **Priority:** Step 8 — donors report suspicious NGOs  
> **Security:** Authenticated users (DONOR) can submit reports

---

## Endpoint

| Method | Path | Body | Response | Auth |
|--------|------|------|----------|------|
| `POST` | `/api/ngos/{id}/report` | `ReportRequest` | `Report` | Authenticated |

---

## File to Create

### `controller/ReportController.java`

**Path:** `src/main/java/com/aidonormatcher/backend/controller/ReportController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.ReportRequest;
import com.aidonormatcher.backend.entity.Report;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/ngos/{id}/report")
    public ResponseEntity<Report> submitReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.submitReport(user.getEmail(), id, request));
    }
}
```

---

## Service Methods Used

Already exists in `ReportService.java`:
- `submitReport(String email, Long ngoId, ReportRequest request)` → creates report, auto-flags NGO if 3+ reports, emails admin

---

## SecurityConfig Change

The report endpoint is under `/api/ngos/{id}/report`. The current rules allow:
```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
```

This only allows GET. POST to `/api/ngos/{id}/report` needs to be explicitly allowed for authenticated users:

```java
.requestMatchers(HttpMethod.POST, "/api/ngos/*/report").authenticated()
```

Add this **before** the general `.anyRequest().authenticated()` line:
```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/ngos/*/report").authenticated()  // ← ADD
```

Alternatively, since `.anyRequest().authenticated()` already catches it, this explicit rule is optional but makes the intent clear.

---

## Business Rules

1. **Auto-flag at 3+ reports** — if an NGO accumulates 3 or more reports, it gets auto-flagged for admin review
2. **Admin notification** — email sent to admin when new report is submitted
3. **Duplicate prevention** — consider adding a check to prevent the same donor from reporting the same NGO multiple times (may need to add this to ReportService)

---

## Testing Checklist

- [ ] `POST /api/ngos/{id}/report` with valid ReportRequest → creates report
- [ ] Report auto-flags NGO at 3+ reports
- [ ] Admin receives email notification
- [ ] Unauthenticated request → 401
- [ ] Invalid NGO ID → appropriate error
