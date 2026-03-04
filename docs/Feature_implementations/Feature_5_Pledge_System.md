# Feature 5 — Pledge System (`PledgeController`)

> **Priority:** Step 5 — donors pledge items against needs  
> **Security:** `ROLE_DONOR` required

---

## Endpoints

| Method | Path | Body / Params | Response | Auth |
|--------|------|---------------|----------|------|
| `POST` | `/api/pledges` | `PledgeRequest` | `PledgeResponse` | DONOR |
| `DELETE` | `/api/pledges/{id}` | — | `204 No Content` | DONOR |
| `GET` | `/api/pledges/donor/active` | — | `List<PledgeResponse>` | DONOR |
| `GET` | `/api/pledges/history` | — | `List<PledgeResponse>` | DONOR |

> **Frontend alignment:** The frontend calls `GET /api/pledges/donor/active` — we use exactly this path.

---

## File to Create

### `controller/PledgeController.java`

**Path:** `src/main/java/com/aidonormatcher/backend/controller/PledgeController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.PledgeRequest;
import com.aidonormatcher.backend.dto.PledgeResponse;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.PledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pledges")
@RequiredArgsConstructor
public class PledgeController {

    private final PledgeService pledgeService;

    @PostMapping
    public ResponseEntity<PledgeResponse> createPledge(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PledgeRequest request) {
        return ResponseEntity.ok(pledgeService.createPledge(user.getEmail(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPledge(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        pledgeService.cancelPledge(user.getEmail(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/donor/active")
    public ResponseEntity<List<PledgeResponse>> getActivePledges(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getActivePledges(user.getEmail()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PledgeResponse>> getPledgeHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getPledgeHistory(user.getEmail()));
    }
}
```

---

## Service Methods Used

All already exist in `PledgeService.java`:
- `createPledge(String email, PledgeRequest request)` → validates email verified, pessimistic lock on need, creates pledge, emails NGO
- `cancelPledge(String email, Long pledgeId)` → validates ownership, cancels, recalculates need status
- `getActivePledges(String email)` → returns ACTIVE pledges as `PledgeResponse`
- `getPledgeHistory(String email)` → returns ALL pledges as `PledgeResponse`

---

## SecurityConfig Rules

Already covered:
```java
.requestMatchers("/api/pledges/**").hasRole("DONOR")
```

---

## Business Rules

1. **Email verification gate** — donor must have `emailVerified = true` to pledge
2. **Pessimistic lock** — `findByIdWithLock` prevents race conditions on quantity
3. **Auto-recalculate** — after pledge/cancel, `recalculateStatus()` updates need status (OPEN → PARTIALLY_PLEDGED → FULLY_PLEDGED)
4. **Email notifications** — NGO emailed on new pledge; donor emailed on cancel

---

## Testing Checklist

- [ ] `POST /api/pledges` with valid request → creates pledge, returns PledgeResponse
- [ ] `POST /api/pledges` with unverified email → 400 error
- [ ] `POST /api/pledges` with quantity > remaining → 400 error
- [ ] `DELETE /api/pledges/{id}` → cancels pledge, recalculates need status
- [ ] `DELETE /api/pledges/{id}` for another donor's pledge → 400 error
- [ ] `GET /api/pledges/donor/active` → returns only ACTIVE pledges
- [ ] `GET /api/pledges/history` → returns all pledges (all statuses)
- [ ] All endpoints return 403 for NGO/ADMIN roles
