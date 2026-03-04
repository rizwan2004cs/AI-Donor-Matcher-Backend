# Feature 8 — Report System (on `NgoController`)

> **Priority:** Step 8 in build order (last)  
> **Security:** Any authenticated user can submit a report  
> **FEATURES.md:** Feature 2.3 — listed under "NGO Public Discovery"  
> **Note:** This endpoint is added to `NgoController.java`, not a separate controller.

---

## Endpoint

| Method | Path | Body | Response | Auth | Status |
|--------|------|------|----------|------|--------|
| `POST` | `/api/ngos/{id}/report` | `{ reason }` | `200 "Report submitted."` | Any | 🔧 |

---

## Addition to `controller/NgoController.java`

Add this method to the existing NgoController:

```java
// ─── Report (any authenticated user) ─────────────────────────────────────

@PostMapping("/api/ngos/{id}/report")
public ResponseEntity<String> submitReport(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @RequestBody Map<String, String> body) {
    reportService.submitReport(id, body.get("reason"), user.getId());
    return ResponseEntity.ok("Report submitted.");
}
```

**Additional dependency to inject in NgoController:**
```java
private final ReportService reportService;
```

**Additional imports:**
```java
import com.aidonormatcher.backend.service.ReportService;
import java.util.Map;
```

---

## Actual Service Method Signature

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `submitReport(ngoId, reason, userId)` | `reportService.submitReport(Long ngoId, String reason, Long reporterUserId)` | `void` |

> **Important:** `ReportService.submitReport` takes three parameters: `ngoId`, `reason` (String), and `reporterUserId` (Long). It does **not** take a `ReportRequest` DTO — the reason is passed as a plain string.

---

## Service Behaviour (from ReportService.java)

1. Loads reporter User by ID
2. Loads NGO by ID
3. Creates and saves `Report(reporter, ngo, reason, reportedAt = now())`
4. Sends confirmation email to the reporter
5. **Auto-flag at 3+ reports:** Counts reports for this NGO. If `>= 3`, finds an ADMIN user and sends `sendAdminReportFlagEmail(admin, ngo)` alert

---

## ReportRequest DTO

The existing `ReportRequest.java` is a simple record:
```java
public record ReportRequest(String reason) {}
```

However, the actual `ReportService.submitReport()` takes `String reason` directly (not the DTO). The controller extracts the reason from request body as `Map<String, String>` or you can use the DTO:

**Alternative using ReportRequest:**
```java
@PostMapping("/api/ngos/{id}/report")
public ResponseEntity<String> submitReport(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody ReportRequest request) {
    reportService.submitReport(id, request.reason(), user.getId());
    return ResponseEntity.ok("Report submitted.");
}
```

---

## SecurityConfig Change

The POST to `/api/ngos/{id}/report` needs authentication but is not role-restricted (both donors and admins can report). Current rules:

```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()  // only GET is public
```

POST to `/api/ngos/*/report` falls through to `.anyRequest().authenticated()`, which **already works**. However, for explicit clarity, add:

```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/ngos/*/report").authenticated()  // ← ADD (optional, explicit)
```

---

## Testing Checklist

- [ ] `POST /api/ngos/{id}/report` with `{ "reason": "Suspicious activity" }` → 200
- [ ] Report saved in database with correct reporter, ngo, reason, timestamp
- [ ] Reporter receives confirmation email
- [ ] Submit 3 reports for same NGO → admin receives flag alert email
- [ ] Unauthenticated request → 401
- [ ] Invalid NGO ID → 400 "NGO not found."
