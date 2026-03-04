# Feature 2 — Nearby Needs Discovery (`NeedController` — public)

> **Priority:** Step 2 — the frontend's main map feed  
> **Security:** Public endpoint (no JWT required)  
> **This is the BIGGEST change** — requires new DTO, new repository query, new service method, new controller endpoint, and a SecurityConfig update.

---

## Endpoint

| Method | Path | Params | Response | Auth |
|--------|------|--------|----------|------|
| `GET` | `/api/needs/nearby` | `lat`, `lng`, `radius?`, `category?` | `List<NearbyNeedDTO>` | No |

**Frontend call:** `GET /api/needs/nearby?lat=12.97&lng=77.59&radius=25&category=FOOD`

If `lat`/`lng` are omitted → returns all active needs from live NGOs (no radius filter, used for auto-expand fallback).

---

## Files to Create / Modify

### 1. NEW — `dto/NearbyNeedDTO.java`

**Path:** `src/main/java/com/aidonormatcher/backend/dto/NearbyNeedDTO.java`

```java
package com.aidonormatcher.backend.dto;

public record NearbyNeedDTO(
        Long needId,
        String itemName,
        String description,
        String category,
        String urgency,
        int quantityRequired,
        int quantityPledged,
        int quantityRemaining,
        String expiryDate,
        Long ngoId,
        String ngoName,
        String ngoPhotoUrl,
        int trustScore,
        String trustTier,
        Double ngoLat,
        Double ngoLng,
        double distanceKm
) {}
```

---

### 2. MODIFY — `repository/NeedRepository.java`

Add this native query method:

```java
@Query(value = """
    SELECT n.*,
    (6371 * acos(
        cos(radians(:lat)) * cos(radians(ng.lat)) *
        cos(radians(ng.lng) - radians(:lng)) +
        sin(radians(:lat)) * sin(radians(ng.lat))
    )) AS distance_km
    FROM needs n
    JOIN ngos ng ON n.ngo_id = ng.id
    WHERE ng.status = 'APPROVED'
      AND ng.profile_complete = true
      AND n.status IN ('OPEN', 'PARTIALLY_PLEDGED')
      AND (:category IS NULL OR n.category = :category)
    HAVING distance_km <= :radius
    ORDER BY distance_km ASC
    """, nativeQuery = true)
List<Object[]> findNearbyNeeds(
    @Param("lat") double lat,
    @Param("lng") double lng,
    @Param("radius") double radius,
    @Param("category") String category
);

@Query(value = """
    SELECT n.*,
    0.0 AS distance_km
    FROM needs n
    JOIN ngos ng ON n.ngo_id = ng.id
    WHERE ng.status = 'APPROVED'
      AND ng.profile_complete = true
      AND n.status IN ('OPEN', 'PARTIALLY_PLEDGED')
      AND (:category IS NULL OR n.category = :category)
    ORDER BY n.urgency DESC, n.created_at ASC
    """, nativeQuery = true)
List<Object[]> findAllActiveNeeds(
    @Param("category") String category
);
```

---

### 3. MODIFY — `service/NeedService.java`

Add this method:

```java
public List<NearbyNeedDTO> findNearbyNeeds(Double lat, Double lng,
                                            Double radius, String category) {
    List<Object[]> rows;

    if (lat == null || lng == null) {
        // No coordinates → return all active needs from live NGOs
        rows = needRepository.findAllActiveNeeds(category);
    } else {
        double rad = (radius != null) ? radius : 50.0; // default 50km
        rows = needRepository.findNearbyNeeds(lat, lng, rad, category);
    }

    return rows.stream().map(row -> {
        // row[0..N] = need columns, last column = distance_km
        // Because this is a native query returning Need + computed column,
        // we cast by index. The exact indices depend on column order in the table.
        // Safer approach: use EntityManager or a result-set mapping.
        //
        // Pragmatic approach: load Need entity + distance from raw row.
        Need need = needRepository.findById(((Number) row[0]).longValue()).orElse(null);
        if (need == null) return null;
        double distanceKm = ((Number) row[row.length - 1]).doubleValue();
        Ngo ngo = need.getNgo();

        return new NearbyNeedDTO(
                need.getId(),
                need.getItemName(),
                need.getDescription(),
                need.getCategory().name(),
                need.getUrgency().name(),
                need.getQuantityRequired(),
                need.getQuantityPledged(),
                need.getQuantityRemaining(),
                need.getExpiryDate() != null ? need.getExpiryDate().toString() : null,
                ngo.getId(),
                ngo.getName(),
                ngo.getPhotoUrl(),
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : "NEW",
                ngo.getLat(),
                ngo.getLng(),
                distanceKm
        );
    }).filter(java.util.Objects::nonNull).toList();
}
```

**Required import additions:**
```java
import com.aidonormatcher.backend.dto.NearbyNeedDTO;
import com.aidonormatcher.backend.entity.Ngo;
```

---

### 4. NEW — `controller/NeedController.java`

**Path:** `src/main/java/com/aidonormatcher/backend/controller/NeedController.java`

```java
package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NearbyNeedDTO;
import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.User;
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

    // ─── PUBLIC: Nearby Needs Discovery ──────────────────────────────────────

    /**
     * GET /api/needs/nearby?lat=&lng=&radius=&category=
     *
     * Returns active needs from approved, profile-complete NGOs
     * sorted by distance from the donor's location.
     * If lat/lng omitted → returns all active needs (auto-expand fallback).
     */
    @GetMapping("/api/needs/nearby")
    public ResponseEntity<List<NearbyNeedDTO>> getNearbyNeeds(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(needService.findNearbyNeeds(lat, lng, radius, category));
    }

    // ─── NGO-ONLY: Needs CRUD (see Feature 4) ───────────────────────────────
    // Endpoints below are added in Step 4 (Feature_4_Needs_Management.md)
}
```

---

### 5. MODIFY — `config/SecurityConfig.java`

Add this rule **before** the `.anyRequest().authenticated()` line:

```java
.requestMatchers(HttpMethod.GET, "/api/needs/nearby").permitAll()
```

Full updated authorize block:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/needs/nearby").permitAll()  // ← ADD
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/ngo/**").hasRole("NGO")
    .requestMatchers("/api/pledges/**").hasRole("DONOR")
    .anyRequest().authenticated()
)
```

---

## Alternative: Simpler Service Mapping (if native query indices are fragile)

Instead of parsing `Object[]` by index, use `@SqlResultSetMapping` or a simpler approach: query only need IDs + distances, then load full entities:

```java
// In NeedRepository — return only (need_id, distance_km) pairs
@Query(value = """
    SELECT n.id AS need_id,
    (6371 * acos(...)) AS distance_km
    FROM needs n JOIN ngos ng ON n.ngo_id = ng.id
    WHERE ...
    HAVING distance_km <= :radius
    ORDER BY distance_km ASC
    """, nativeQuery = true)
List<Object[]> findNearbyNeedIds(...);
```

Then in service: load each Need by ID — avoids fragile column-index mapping.

---

## Testing Checklist

- [ ] `GET /api/needs/nearby?lat=12.97&lng=77.59&radius=25` → returns needs within 25km
- [ ] `GET /api/needs/nearby?lat=12.97&lng=77.59&radius=25&category=FOOD` → filtered by category
- [ ] `GET /api/needs/nearby` (no params) → returns all active needs
- [ ] Response includes need details + NGO name, photo, lat/lng, trustScore
- [ ] Needs from SUSPENDED/PENDING NGOs are excluded
- [ ] Needs with status FULFILLED/EXPIRED are excluded
- [ ] No JWT required (public endpoint)
