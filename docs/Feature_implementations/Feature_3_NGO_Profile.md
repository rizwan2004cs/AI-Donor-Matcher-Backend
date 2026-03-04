# Feature 3 — NGO Profile Management (`NgoController` — NGO-role endpoints)

> **Priority:** Step 2 in build order — NGOs must configure profile before appearing on map  
> **Security:** `ROLE_NGO` required for all endpoints  
> **Depends on:** `NgoService`, `CloudinaryService`  
> **New files:** 1 controller (also extended in Features 2 and 8)

---

## Endpoints

| Method | Path | Body / Params | Response | Auth | Status |
|--------|------|---------------|----------|------|--------|
| `GET` | `/api/ngo/my/profile` | — | `Ngo` | NGO | 🔧 |
| `PUT` | `/api/ngo/my/profile` | `NgoProfileRequest` | `Ngo` | NGO | 🔧 |
| `POST` | `/api/ngo/my/photo` | `MultipartFile` | `{ url }` | NGO | 🔧 |

---

## File to Create

**Path:** `src/main/java/com/aidonormatcher/backend/controller/NgoController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.CloudinaryService;
import com.aidonormatcher.backend.service.NgoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NgoController {

    private final NgoService ngoService;
    private final CloudinaryService cloudinaryService;

    // ─── NGO: Own Profile ────────────────────────────────────────────────────

    @GetMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ngoService.getMyProfile(user.getEmail()));
    }

    @PutMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NgoProfileRequest request) {
        return ResponseEntity.ok(ngoService.updateProfile(user.getEmail(), request));
    }

    @PostMapping("/api/ngo/my/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.uploadPhoto(file);
        ngoService.updatePhotoUrl(user.getEmail(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ─── PUBLIC: Discovery + View (added in Feature_2_NGO_Discovery.md) ──────
    // ─── Report endpoint (added in Feature_8_Report_System.md) ───────────────
}
```

---

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `getMyProfile(email)` | `ngoService.getMyProfile(String email)` | `Ngo` |
| `updateProfile(email, req)` | `ngoService.updateProfile(String email, NgoProfileRequest req)` | `Ngo` |
| `uploadPhoto` → two calls | `cloudinaryService.uploadPhoto(MultipartFile)` → `String url`; then `ngoService.updatePhotoUrl(String email, String url)` → `void` |

---

## NgoProfileRequest (already exists)

```java
public record NgoProfileRequest(
    String name,
    String address,
    String contactEmail,
    String contactPhone,
    String description,       // min 50 characters per spec
    NeedCategory categoryOfWork
) {}
```

---

## Service Behaviour (from NgoService.java)

**`updateProfile`:**
1. Finds User by email → finds Ngo by User
2. Updates non-null fields: name, address, contactEmail, contactPhone, description, categoryOfWork
3. Runs `checkProfileComplete(ngo)` → sets `profileComplete = true` if all required fields are filled
4. If address changed → calls `geocodeAddress(ngo)` (Nominatim free API) to set `lat`/`lng`
5. Saves and calls `trustScoreService.recalculate(ngo)`

**Profile completeness check (go-live gate):**
```java
private boolean checkProfileComplete(Ngo ngo) {
    return ngo.getName() != null && !ngo.getName().isBlank()
        && ngo.getAddress() != null && !ngo.getAddress().isBlank()
        && ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()
        && ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()
        && ngo.getDescription() != null && ngo.getDescription().length() >= 50
        && ngo.getCategoryOfWork() != null;
}
```

**Go-live condition:** An NGO appears on the donor map **only** when `status = APPROVED AND profileComplete = true`. Enforced in the Haversine query.

**Photo upload:**
- JPG/PNG, max 2MB (configured in `application.properties`)
- Hosted on Cloudinary free tier
- `CloudinaryService.uploadPhoto(file)` returns the URL string

---

## SecurityConfig Rules (already in place)

```java
.requestMatchers("/api/ngo/**").hasRole("NGO")
```

No changes needed.

---

## Testing Checklist

- [ ] `GET /api/ngo/my/profile` → returns Ngo entity for authenticated NGO user
- [ ] `PUT /api/ngo/my/profile` with all required fields → `profileComplete=true`
- [ ] `PUT /api/ngo/my/profile` with address → geocodes to lat/lng
- [ ] `PUT /api/ngo/my/profile` with partial fields → `profileComplete=false`
- [ ] `POST /api/ngo/my/photo` with multipart file → uploads to Cloudinary, returns `{ "url": "..." }`
- [ ] `GET /api/ngo/my/profile` as DONOR → 403 Forbidden
- [ ] Trust score recalculated after profile update
