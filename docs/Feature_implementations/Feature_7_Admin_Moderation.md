# Feature 7 — Admin Moderation (`AdminController` — Part 2)

> **Priority:** Step 7 — admins moderate content and view stats  
> **Security:** `ROLE_ADMIN` required  
> **Note:** These endpoints are **added** to the `AdminController.java` created in Feature 6.

---

## Endpoints

| Method | Path | Body / Params | Response | Auth |
|--------|------|---------------|----------|------|
| `GET` | `/api/admin/reports` | — | `List<Report>` | ADMIN |
| `PUT` | `/api/admin/needs/{id}` | `NeedRequest` | `Need` | ADMIN |
| `DELETE` | `/api/admin/needs/{id}` | — | `204 No Content` | ADMIN |
| `GET` | `/api/admin/stats` | — | `Map<String, Object>` | ADMIN |

---

## Additions to `controller/AdminController.java`

Add these methods to the existing AdminController:

```java
// ─── Moderation ──────────────────────────────────────────────────────────

@GetMapping("/reports")
public ResponseEntity<List<Report>> getReports() {
    return ResponseEntity.ok(adminService.getReports());
}

@PutMapping("/needs/{id}")
public ResponseEntity<Need> editNeed(
        @PathVariable Long id,
        @Valid @RequestBody NeedRequest request) {
    return ResponseEntity.ok(adminService.editNeed(id, request));
}

@DeleteMapping("/needs/{id}")
public ResponseEntity<Void> removeNeed(@PathVariable Long id) {
    adminService.removeNeed(id);
    return ResponseEntity.noContent().build();
}

// ─── Dashboard Stats ─────────────────────────────────────────────────────

@GetMapping("/stats")
public ResponseEntity<Map<String, Object>> getStats() {
    return ResponseEntity.ok(adminService.getStats());
}
```

---

## Service Methods Used

All already exist in `AdminService.java`:
- `getReports()` → returns all reports
- `editNeed(Long id, NeedRequest request)` → admin override edit (no lock guard)
- `removeNeed(Long id)` → deletes need + cascades pledge cancellations, emails NGO + donors
- `getStats()` → returns `Map` with counts: totalDonors, totalNgos, totalNeeds, totalPledges, pendingNgos, activeNeeds, etc.

---

## Stats Response Shape

The frontend expects:
```json
{
  "totalDonors": 42,
  "totalNgos": 15,
  "pendingNgos": 3,
  "approvedNgos": 10,
  "suspendedNgos": 2,
  "totalNeeds": 87,
  "activeNeeds": 45,
  "fulfilledNeeds": 30,
  "totalPledges": 120,
  "activePledges": 55,
  "totalReports": 8
}
```

If `AdminService.getStats()` doesn't return all these keys, it may need to be extended.

---

## SecurityConfig Rules

Already covered:
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

---

## Testing Checklist

- [ ] `GET /api/admin/reports` → returns all reports
- [ ] `PUT /api/admin/needs/{id}` → admin can edit any need (no lock guard)
- [ ] `DELETE /api/admin/needs/{id}` → cascades pledge cancellations
- [ ] `GET /api/admin/stats` → returns dashboard statistics map
- [ ] All endpoints return 403 for non-ADMIN roles
