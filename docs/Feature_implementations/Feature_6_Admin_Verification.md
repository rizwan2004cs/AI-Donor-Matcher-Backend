# Feature 6 — Admin NGO Verification (`AdminController` — Part 1)

> **Priority:** Step 6 — admins approve/reject/suspend NGOs  
> **Security:** `ROLE_ADMIN` required

---

## Endpoints

| Method | Path | Body / Params | Response | Auth |
|--------|------|---------------|----------|------|
| `GET` | `/api/admin/ngos/pending` | — | `List<Ngo>` | ADMIN |
| `GET` | `/api/admin/ngos` | — | `List<Ngo>` | ADMIN |
| `PATCH` | `/api/admin/ngos/{id}/approve` | — | `Ngo` | ADMIN |
| `PATCH` | `/api/admin/ngos/{id}/reject` | — | `Ngo` | ADMIN |
| `PATCH` | `/api/admin/ngos/{id}/suspend` | — | `Ngo` | ADMIN |

---

## File to Create

### `controller/AdminController.java`

**Path:** `src/main/java/com/aidonormatcher/backend/controller/AdminController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─── NGO Verification (Feature 6) ────────────────────────────────────────

    @GetMapping("/ngos/pending")
    public ResponseEntity<List<Ngo>> getPendingNgos() {
        return ResponseEntity.ok(adminService.getPendingNgos());
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<Ngo>> getAllNgos() {
        return ResponseEntity.ok(adminService.getAllNgos());
    }

    @PatchMapping("/ngos/{id}/approve")
    public ResponseEntity<Ngo> approveNgo(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveNgo(id));
    }

    @PatchMapping("/ngos/{id}/reject")
    public ResponseEntity<Ngo> rejectNgo(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectNgo(id));
    }

    @PatchMapping("/ngos/{id}/suspend")
    public ResponseEntity<Ngo> suspendNgo(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.suspendNgo(id));
    }

    // ─── Moderation (Feature 7) — added in Feature_7_Admin_Moderation.md ────
}
```

---

## Service Methods Used

All already exist in `AdminService.java`:
- `getPendingNgos()` → returns NGOs with status PENDING
- `getAllNgos()` → returns all NGOs
- `approveNgo(Long id)` → sets status APPROVED, emails NGO, recalculates trust score
- `rejectNgo(Long id)` → sets status PENDING (or a rejected state), emails NGO
- `suspendNgo(Long id)` → sets status SUSPENDED, cascades: expires all active needs and cancels all active pledges, emails NGO + affected donors

---

## SecurityConfig Rules

Already covered:
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

---

## Business Rules

1. **Approve** → triggers trust score recalculation, sends approval email
2. **Reject** → sends rejection email with reason
3. **Suspend cascade:**
   - All NGO's OPEN/PARTIALLY_PLEDGED needs → set to EXPIRED
   - All ACTIVE pledges on those needs → set to CANCELLED
   - Email sent to NGO and each affected donor

---

## Testing Checklist

- [ ] `GET /api/admin/ngos/pending` → returns only PENDING NGOs
- [ ] `GET /api/admin/ngos` → returns all NGOs
- [ ] `PATCH /api/admin/ngos/{id}/approve` → sets APPROVED, trust score recalculated
- [ ] `PATCH /api/admin/ngos/{id}/reject` → sets status back, email sent
- [ ] `PATCH /api/admin/ngos/{id}/suspend` → cascades needs/pledges, emails sent
- [ ] All endpoints return 403 for non-ADMIN roles
