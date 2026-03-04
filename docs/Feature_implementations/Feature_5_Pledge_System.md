# Feature 5 — Pledge System (`PledgeController`)

> **Priority:** Step 5 in build order — core donation loop  
> **Security:** `ROLE_DONOR` required  
> **Depends on:** `PledgeService`, `NeedService`

---

## Endpoints

| Method | Path | Body / Params | Response | Auth | Status |
|--------|------|---------------|----------|------|--------|
| `POST` | `/api/pledges` | `PledgeRequest` | `PledgeResponse` | DONOR | 🔧 |
| `DELETE` | `/api/pledges/{id}` | — | `204 No Content` | DONOR | 🔧 |
| `GET` | `/api/pledges/active` | — | `List<Pledge>` | DONOR | 🔧 |
| `GET` | `/api/pledges/history` | — | `List<Pledge>` | DONOR | 🔧 |

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/PledgeController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.PledgeRequest;
import com.aidonormatcher.backend.dto.PledgeResponse;
import com.aidonormatcher.backend.entity.Pledge;
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
        return ResponseEntity.ok(pledgeService.createPledge(request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPledge(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        pledgeService.cancelPledge(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Pledge>> getActivePledges(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getActivePledges(user.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Pledge>> getPledgeHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getPledgeHistory(user.getId()));
    }
}
```

---

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `createPledge(req, userId)` | `pledgeService.createPledge(PledgeRequest req, Long donorId)` | `PledgeResponse` |
| `cancelPledge(pledgeId, userId)` | `pledgeService.cancelPledge(Long pledgeId, Long donorId)` | `void` |
| `getActivePledges(userId)` | `pledgeService.getActivePledges(Long donorId)` | `List<Pledge>` |
| `getPledgeHistory(userId)` | `pledgeService.getPledgeHistory(Long donorId)` | `List<Pledge>` |

> **Note:** `createPledge` returns `PledgeResponse` (with NGO coordinates for navigation).  
> `getActivePledges` and `getPledgeHistory` return `List<Pledge>` (full entity), not `List<PledgeResponse>`.

---

## DTOs (already exist)

**`PledgeRequest`:**
```java
public record PledgeRequest(
    Long needId,
    int quantity
) {}
```

**`PledgeResponse`** (returned only by createPledge):
```java
public record PledgeResponse(
    Long pledgeId,
    Double ngoLat,
    Double ngoLng,
    String ngoAddress,
    String ngoContactEmail,
    LocalDateTime expiresAt
) {}
```

---

## Service Behaviour (from PledgeService.java)

**`createPledge`:**
1. Loads donor by ID → checks `emailVerified == true` (gate: "Email not verified.")
2. Loads need with **pessimistic write lock** (`findByIdWithLock`) → prevents race conditions
3. Validates need status is OPEN or PARTIALLY_PLEDGED
4. Validates `req.quantity() <= need.getQuantityRemaining()`
5. Creates `Pledge(donor, need, quantity, ACTIVE, createdAt, expiresAt = now + 48h)`
6. Updates `need.quantityPledged += quantity` → calls `recalculateStatus()`
7. Updates `ngo.lastActivityAt`
8. Sends confirmation email to donor (includes NGO address + contact)
9. Returns `PledgeResponse` with NGO coordinates for in-app navigation

**`cancelPledge`:**
1. Validates ownership (`pledge.getDonor().getId().equals(donorId)`)
2. Validates status is ACTIVE
3. Sets status to CANCELLED
4. Restores quantity: `need.quantityPledged -= pledge.quantity`
5. Calls `recalculateStatus()` on need
6. Emails NGO about cancellation

**48-hour auto-expiry:** `ScheduledJobService.expireOldPledges()` runs hourly. Expired pledges → status CANCELLED, quantity restored, donor notified.

---

## Donor Journey (from AI_Donation_Matcher_FINAL_v4_.md)

1. **Pledge** → Donor sees quantity selector (total required, already pledged, remaining). Quan capped at remaining.
2. **Navigation** → After pledging, Delivery View opens with Leaflet map showing OSRM route to NGO. Distance + travel time displayed. NGO contact email shown.
3. **Active Dashboard** → Shows all active pledges: NGO name, item, quantity, pledge status, 48h countdown, "Navigate" button, "Cancel Pledge" button.
4. **Cancel** → Restores quantity, notifies NGO.
5. **Fulfillment** → NGO marks as Fulfilled → donor gets thank-you email → pledge moves to History tab.

---

## SecurityConfig Rules (already in place)

```java
.requestMatchers("/api/pledges/**").hasRole("DONOR")
```

No changes needed.

---

## Testing Checklist

- [ ] `POST /api/pledges` with valid request → creates pledge, returns PledgeResponse with NGO coordinates
- [ ] `POST /api/pledges` with unverified email → 400 "Email not verified."
- [ ] `POST /api/pledges` with quantity > remaining → 400 error
- [ ] `POST /api/pledges` on FULFILLED need → 400 "no longer available"
- [ ] `DELETE /api/pledges/{id}` → cancels pledge, restores quantity, recalculates need status
- [ ] `DELETE /api/pledges/{id}` for another donor's pledge → 400 "Unauthorized"
- [ ] `DELETE /api/pledges/{id}` on already cancelled pledge → 400
- [ ] `GET /api/pledges/active` → returns only ACTIVE pledges for this donor
- [ ] `GET /api/pledges/history` → returns all pledges (all statuses)
- [ ] All endpoints return 403 for NGO/ADMIN roles
