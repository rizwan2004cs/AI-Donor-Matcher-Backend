# Feature 6 — Admin NGO Verification (`AdminController` — Part 1)

> **Priority:** Step 6 in build order  
> **Security:** `ROLE_ADMIN` required  
> **HTTP Methods:** `POST` for approve/reject/suspend (per BACKEND.md)

---

## Endpoints

| Method | Path | Body / Params | Response | Auth | Status |
|--------|------|---------------|----------|------|--------|
| `GET` | `/api/admin/ngos/pending` | — | `List<Ngo>` | ADMIN | 🔧 |
| `POST` | `/api/admin/ngos/{id}/approve` | — | `200 "NGO approved."` | ADMIN | 🔧 |
| `POST` | `/api/admin/ngos/{id}/reject` | `{ reason }` | `200 "NGO rejected."` | ADMIN | 🔧 |
| `POST` | `/api/admin/ngos/{id}/suspend` | — | `200 "NGO suspended."` | ADMIN | 🔧 |

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/AdminController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
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

    @PostMapping("/ngos/{id}/approve")
    public ResponseEntity<String> approveNgo(@PathVariable Long id) {
        adminService.approveNgo(id);
        return ResponseEntity.ok("NGO approved.");
    }

    @PostMapping("/ngos/{id}/reject")
    public ResponseEntity<String> rejectNgo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        adminService.rejectNgo(id, body.get("reason"));
        return ResponseEntity.ok("NGO rejected.");
    }

    @PostMapping("/ngos/{id}/suspend")
    public ResponseEntity<String> suspendNgo(@PathVariable Long id) {
        adminService.suspendNgo(id);
        return ResponseEntity.ok("NGO suspended.");
    }

    // ─── Moderation + Stats (Feature 7) — see Feature_7_Admin_Moderation.md ──
}
```

---

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `getPendingNgos()` | `adminService.getPendingNgos()` | `List<Ngo>` |
| `getAllNgos()` | `adminService.getAllNgos()` | `List<Ngo>` |
| `approveNgo(id)` | `adminService.approveNgo(Long ngoId)` | `void` |
| `rejectNgo(id, reason)` | `adminService.rejectNgo(Long ngoId, String reason)` | `void` |
| `suspendNgo(id)` | `adminService.suspendNgo(Long ngoId)` | `void` |

> **Important:** All three action methods return `void`, so the controller returns a String message, not the Ngo entity.  
> `rejectNgo` takes a `String reason` parameter — extracted from the request body as `{ "reason": "..." }`.

---

## Service Behaviour (from AdminService.java)

**`approveNgo`:**
1. Finds NGO by ID
2. Sets `status = APPROVED`, `verifiedAt = now()`
3. Saves → calls `trustScoreService.recalculate(ngo)`
4. Sends approval email to NGO

**`rejectNgo`:**
1. Finds NGO by ID
2. Sets `rejectionReason` on the Ngo entity (status remains PENDING — NGO can reapply with corrected docs)
3. Saves → sends rejection email with reason to NGO

**`suspendNgo` (cascade):**
1. Sets `status = SUSPENDED`
2. Finds all active needs (OPEN, PARTIALLY_PLEDGED, FULLY_PLEDGED)
3. For each need:
   - Finds all ACTIVE pledges → sets each to CANCELLED → emails each affected donor
   - Sets need status to EXPIRED
4. Sends suspension notification to NGO

---

## SecurityConfig Rules (already in place)

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

No changes needed.

---

## Testing Checklist

- [ ] `GET /api/admin/ngos/pending` → returns only PENDING NGOs
- [ ] `GET /api/admin/ngos` → returns all NGOs (all statuses)
- [ ] `POST /api/admin/ngos/{id}/approve` → sets APPROVED, trust score recalculated, approval email sent
- [ ] `POST /api/admin/ngos/{id}/reject` with `{ "reason": "..." }` → saves rejectionReason, email sent
- [ ] `POST /api/admin/ngos/{id}/suspend` → cascades: needs expired + pledges cancelled + emails sent
- [ ] All endpoints return 403 for non-ADMIN roles
