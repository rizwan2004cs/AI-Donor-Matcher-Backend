# Feature 3 — NGO Profile Management (`NgoController`)

> **Priority:** Step 3 — NGOs must configure their profile before needs appear  
> **Security:** `ROLE_NGO` required for all endpoints

---

## Endpoints

| Method | Path | Body / Params | Response | Auth |
|--------|------|---------------|----------|------|
| `GET` | `/api/ngo/my/profile` | — | `Ngo` | NGO |
| `PUT` | `/api/ngo/my/profile` | `NgoProfileRequest` | `Ngo` | NGO |
| `POST` | `/api/ngo/my/photo` | `MultipartFile` | `{ url }` | NGO |
| `GET` | `/api/ngos/{id}` | — | `NgoDiscoveryDTO` | Public |

---

## File to Create

### `controller/NgoController.java`

**Path:** `src/main/java/com/aidonormatcher/backend/controller/NgoController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
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

    // ─── PUBLIC: View Single NGO ─────────────────────────────────────────────

    @GetMapping("/api/ngos/{id}")
    public ResponseEntity<NgoDiscoveryDTO> getNgoById(@PathVariable Long id) {
        return ResponseEntity.ok(ngoService.getNgoById(id));
    }
}
```

---

## Service Methods Used

All already exist in `NgoService.java`:
- `getMyProfile(String email)` → returns `Ngo`
- `updateProfile(String email, NgoProfileRequest request)` → geocodes address, sets `profileComplete=true`
- `updatePhotoUrl(String email, String url)` → updates `photoUrl`
- `getNgoById(Long id)` → returns `NgoDiscoveryDTO` (public view)

---

## SecurityConfig Rules

Already covered by existing patterns:
```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()  // public NGO view
.requestMatchers("/api/ngo/**").hasRole("NGO")                // NGO-only profile
```

---

## Testing Checklist

- [ ] `GET /api/ngo/my/profile` → returns NGO entity for authenticated NGO user
- [ ] `PUT /api/ngo/my/profile` with valid `NgoProfileRequest` → updates and returns NGO
- [ ] `PUT /api/ngo/my/profile` geocodes address to lat/lng
- [ ] `POST /api/ngo/my/photo` with multipart file → uploads to Cloudinary, returns URL
- [ ] `GET /api/ngos/{id}` → returns public `NgoDiscoveryDTO` (no auth required)
- [ ] `GET /api/ngo/my/profile` as DONOR → 403 Forbidden
