# Feature 7 — Admin Moderation + Stats (`AdminController` — Part 2)

> **Priority:** Step 6+7 (built together with Feature 6)  
> **Security:** `ROLE_ADMIN` required  
> **Note:** These endpoints are **added** to the `AdminController.java` created in Feature 6.

---

## Endpoints

| Method | Path | Body / Params | Response | Auth | Status |
|--------|------|---------------|----------|------|--------|
| `GET` | `/api/admin/reports` | — | `List<Report>` | ADMIN | 🔧 |
| `PUT` | `/api/admin/needs/{id}` | `NeedRequest` | `Need` | ADMIN | 🔧 |
| `DELETE` | `/api/admin/needs/{id}` | — | `204 No Content` | ADMIN | 🔧 |
| `GET` | `/api/admin/stats` | — | `Map<String, Object>` | ADMIN | 🔧 |

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

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `getReports()` | `adminService.getReports()` | `List<Report>` |
| `editNeed(id, req)` | `adminService.editNeed(Long needId, NeedRequest req)` | `Need` |
| `removeNeed(id)` | `adminService.removeNeed(Long needId)` | `void` |
| `getStats()` | `adminService.getStats()` | `Map<String, Object>` |

---

## Service Behaviour (from AdminService.java)

**`getReports`:** Returns all reports ordered by `reportedAt DESC`.

**`editNeed`:** Admin override — updates need fields directly (no lock guard, no ownership check). Sets category, itemName, description, quantityRequired, urgency, expiryDate.

**`removeNeed` (cascade):**
1. Finds all ACTIVE pledges on the need
2. Sets each pledge to CANCELLED → emails each affected donor
3. Deletes the need

**`getStats`:** Currently returns:
```json
{
  "totalUsers": 42,
  "totalNgos": 15,
  "pendingNgos": 3,
  "totalNeeds": 87,
  "totalPledges": 120,
  "totalReports": 8
}
```

> **Note:** FEATURES.md marks stats as ⬜ (Feature 7.4). The service method **does exist** but may need extending to include additional keys the frontend expects, such as:
> - `approvedNgos`, `suspendedNgos`
> - `activeNeeds`, `fulfilledNeeds`
> - `activePledges`
> - `totalDonors`
>
> Extend `AdminService.getStats()` as needed when building the frontend dashboard.

---

## SecurityConfig Rules (already in place)

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

No changes needed.

---

## Testing Checklist

- [ ] `GET /api/admin/reports` → returns all reports ordered by date
- [ ] `PUT /api/admin/needs/{id}` → admin can edit any need (no lock guard, no ownership check)
- [ ] `DELETE /api/admin/needs/{id}` → cascades: cancels active pledges, emails donors, deletes need
- [ ] `GET /api/admin/stats` → returns statistics map with expected keys
- [ ] All endpoints return 403 for non-ADMIN roles
