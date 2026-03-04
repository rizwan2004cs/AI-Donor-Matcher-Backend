# Feature 4 — Needs Management (`NeedController` — NGO endpoints)

> **Priority:** Step 4 — NGOs create/manage needs that donors pledge against  
> **Security:** `ROLE_NGO` required  
> **Note:** The `NeedController.java` file was created in Feature 2. These endpoints are **added** to it.

---

## Endpoints

| Method | Path | Body / Params | Response | Auth |
|--------|------|---------------|----------|------|
| `GET` | `/api/ngo/my/needs` | — | `List<Need>` | NGO |
| `POST` | `/api/needs` | `NeedRequest` | `Need` | NGO |
| `PUT` | `/api/needs/{id}` | `NeedRequest` | `Need` | NGO |
| `DELETE` | `/api/needs/{id}` | — | `204 No Content` | NGO |
| `PATCH` | `/api/needs/{id}/fulfill` | — | `Need` | NGO |

---

## Modifications to `controller/NeedController.java`

Add these methods below the existing nearby needs endpoint:

```java
// ─── NGO-ONLY: Needs CRUD ────────────────────────────────────────────────

@GetMapping("/api/ngo/my/needs")
public ResponseEntity<List<Need>> getMyNeeds(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(needService.getNeedsByNgo(user.getEmail()));
}

@PostMapping("/api/needs")
public ResponseEntity<Need> createNeed(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody NeedRequest request) {
    return ResponseEntity.ok(needService.createNeed(user.getEmail(), request));
}

@PutMapping("/api/needs/{id}")
public ResponseEntity<Need> updateNeed(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody NeedRequest request) {
    return ResponseEntity.ok(needService.updateNeed(user.getEmail(), id, request));
}

@DeleteMapping("/api/needs/{id}")
public ResponseEntity<Void> deleteNeed(
        @AuthenticationPrincipal User user,
        @PathVariable Long id) {
    needService.deleteNeed(user.getEmail(), id);
    return ResponseEntity.noContent().build();
}

@PatchMapping("/api/needs/{id}/fulfill")
public ResponseEntity<Need> fulfillNeed(
        @AuthenticationPrincipal User user,
        @PathVariable Long id) {
    return ResponseEntity.ok(needService.fulfillNeed(user.getEmail(), id));
}
```

**Additional imports needed in NeedController.java:**
```java
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.dto.NeedRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
```

---

## Service Methods Used

All already exist in `NeedService.java`:
- `getNeedsByNgo(String email)` → returns all needs for the NGO
- `createNeed(String email, NeedRequest request)` → validates max 5 open needs, creates need
- `updateNeed(String email, Long id, NeedRequest request)` → validates ownership, lock guard (no pledges)
- `deleteNeed(String email, Long id)` → validates ownership, lock guard (no pledges), cascades pledge cancellations
- `fulfillNeed(String email, Long id)` → marks as FULFILLED, fulfills active pledges, emails donors

---

## SecurityConfig Rules

Already covered:
```java
.requestMatchers("/api/ngo/**").hasRole("NGO")  // covers /api/ngo/my/needs
```

**Need to add** rules for POST/PUT/DELETE/PATCH on `/api/needs/**` for NGO role:
```java
.requestMatchers(HttpMethod.POST, "/api/needs").hasRole("NGO")
.requestMatchers(HttpMethod.PUT, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.DELETE, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.PATCH, "/api/needs/**").hasRole("NGO")
```

Place these **after** the public GET rule:
```java
.requestMatchers(HttpMethod.GET, "/api/needs/nearby").permitAll()
.requestMatchers(HttpMethod.POST, "/api/needs").hasRole("NGO")
.requestMatchers(HttpMethod.PUT, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.DELETE, "/api/needs/**").hasRole("NGO")
.requestMatchers(HttpMethod.PATCH, "/api/needs/**").hasRole("NGO")
```

---

## Business Rules

1. **Max 5 open needs** per NGO — `createNeed` enforces this
2. **Lock guard** — cannot update/delete needs that have active pledges
3. **Cascade on delete** — cancels all active pledges and emails donors
4. **Fulfill flow** — marks need as FULFILLED, marks all ACTIVE pledges as FULFILLED, emails each donor

---

## Testing Checklist

- [ ] `POST /api/needs` → creates need for authenticated NGO
- [ ] `POST /api/needs` when 5 open needs exist → 400 error
- [ ] `PUT /api/needs/{id}` → updates when no pledges exist
- [ ] `PUT /api/needs/{id}` when pledges exist → 400 error (locked)
- [ ] `DELETE /api/needs/{id}` → cascades pledge cancellations
- [ ] `PATCH /api/needs/{id}/fulfill` → marks as FULFILLED + fulfills pledges
- [ ] `GET /api/ngo/my/needs` → returns only the NGO's own needs
- [ ] All endpoints return 403 for DONOR role
