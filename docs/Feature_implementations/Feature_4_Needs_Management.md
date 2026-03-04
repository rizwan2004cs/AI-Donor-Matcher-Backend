# Feature 4 — Needs Management (`NeedController`)

> **Priority:** Step 3 in build order — NGOs create needs that donors pledge against  
> **Security:** `ROLE_NGO` required  
> **New files:** 1 controller + SecurityConfig modification

---

## Endpoints

| Method | Path | Body / Params | Response | Auth | Status |
|--------|------|---------------|----------|------|--------|
| `GET` | `/api/ngo/my/needs` | — | `List<Need>` | NGO | 🔧 |
| `POST` | `/api/needs` | `NeedRequest` | `Need` | NGO | 🔧 |
| `PUT` | `/api/needs/{id}` | `NeedRequest` | `Need` | NGO | 🔧 |
| `DELETE` | `/api/needs/{id}` | — | `204 No Content` | NGO | 🔧 |
| `PATCH` | `/api/needs/{id}/fulfill` | — | `204 No Content` | NGO | 🔧 |

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/NeedController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.service.NeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NeedController {

    private final NeedService needService;
    private final NgoRepository ngoRepository;

    // ─── NGO-ONLY: Needs CRUD ────────────────────────────────────────────────

    @GetMapping("/api/ngo/my/needs")
    public ResponseEntity<List<Need>> getMyNeeds(@AuthenticationPrincipal User user) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.getNeedsByNgo(ngo));
    }

    @PostMapping("/api/needs")
    public ResponseEntity<Need> createNeed(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NeedRequest request) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.createNeed(request, ngo.getId()));
    }

    @PutMapping("/api/needs/{id}")
    public ResponseEntity<Need> updateNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody NeedRequest request) {
        return ResponseEntity.ok(needService.updateNeed(id, request, user.getId()));
    }

    @DeleteMapping("/api/needs/{id}")
    public ResponseEntity<Void> deleteNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.deleteNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/needs/{id}/fulfill")
    public ResponseEntity<Void> fulfillNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.fulfillNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
```

---

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `getMyNeeds` | `needService.getNeedsByNgo(Ngo ngo)` | `List<Need>` |
| `createNeed` | `needService.createNeed(NeedRequest req, Long ngoId)` | `Need` |
| `updateNeed` | `needService.updateNeed(Long needId, NeedRequest req, Long ngoUserId)` | `Need` |
| `deleteNeed` | `needService.deleteNeed(Long needId, Long ngoUserId)` | `void` |
| `fulfillNeed` | `needService.fulfillNeed(Long needId, Long ngoUserId)` | `void` |

> **Important:** `createNeed` takes `ngoId` (the Ngo entity ID), while `updateNeed`/`deleteNeed`/`fulfillNeed` take `ngoUserId` (the User ID) — they internally verify ownership by checking `need.getNgo().getUser().getId().equals(ngoUserId)`.  
> For `getNeedsByNgo`, we pass the full `Ngo` entity, so we resolve it first via `ngoRepository.findByUserId()`.

---

## NeedRequest DTO (already exists)

```java
public record NeedRequest(
    NeedCategory category,
    String itemName,
    String description,
    int quantityRequired,
    UrgencyLevel urgency,
    LocalDate expiryDate          // optional
) {}
```

---

## SecurityConfig Changes

The current config only covers `/api/ngo/**` for NGO role. Needs CRUD endpoints at `/api/needs/**` need explicit rules:

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
// ── ADD THESE ──
.requestMatchers(HttpMethod.POST, "/api/needs").hasRole("NGO")
.requestMatchers(HttpMethod.PUT, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.DELETE, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.PATCH, "/api/needs/**").hasRole("NGO")
// ────────────────
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/ngo/**").hasRole("NGO")
.requestMatchers("/api/pledges/**").hasRole("DONOR")
.anyRequest().authenticated()
```

---

## Business Rules (from BACKEND.md + AI_Donation_Matcher_FINAL_v4_.md)

1. **Max 5 active needs** per NGO — `createNeed` enforces. On reaching cap, frontend disables "Add" button with explanatory message.
2. **Lock guard** — cannot edit/delete needs that have `quantityPledged > 0`. Locked needs show a lock icon in the NGO dashboard; only "Mark as Fulfilled" is available.
3. **Cascade on delete** — cancels all active pledges and emails donors (handled by AdminService.removeNeed, not NeedService.deleteNeed which only allows deletion if no pledges).
4. **Fulfill flow** — marks need as FULFILLED with `fulfilledAt` timestamp. Donors receive thank-you emails.
5. **Expiry date** — optional. If set, `ScheduledJobService` auto-closes needs past their expiry date (daily at midnight). Needs approaching expiry within 3 days trigger a warning email to the NGO.

---

## Testing Checklist

- [ ] `POST /api/needs` → creates need for authenticated NGO, returns Need entity
- [ ] `POST /api/needs` when 5 open needs exist → 400 "Maximum 5 active needs reached."
- [ ] `PUT /api/needs/{id}` → updates when `quantityPledged == 0`
- [ ] `PUT /api/needs/{id}` when pledges exist → 400 "locked because it has active pledges"
- [ ] `DELETE /api/needs/{id}` → deletes when no pledges exist
- [ ] `DELETE /api/needs/{id}` when pledges exist → 400 "locked"
- [ ] `PATCH /api/needs/{id}/fulfill` → sets status to FULFILLED + `fulfilledAt` timestamp
- [ ] `GET /api/ngo/my/needs` → returns only this NGO's needs
- [ ] All endpoints return 403 for DONOR role
- [ ] All endpoints verify ownership (another NGO's need → 400 "Unauthorized")
