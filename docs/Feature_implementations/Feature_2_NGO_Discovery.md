# Feature 2 — NGO Public Discovery (`NgoController` — public endpoints)

> **Priority:** Step 4 in build order (after Auth → NGO Profile → Needs)  
> **Security:** Public endpoints (no JWT required)  
> **Depends on:** `NgoService.discoverNgos()`, `NgoRepository.findNearby()`, `NgoRepository.findAllLive()`  
> **Uses existing:** `NgoDiscoveryDTO` — no new DTOs needed

---

## Endpoints

| Method | Path | Params | Response | Auth | Status |
|--------|------|--------|----------|------|--------|
| `GET` | `/api/ngos` | `lat?`, `lng?`, `radius?`, `category?`, `search?` | `List<NgoDiscoveryDTO>` | No | 🔧 |
| `GET` | `/api/ngos/{id}` | — | `Ngo` | No | 🔧 |

---

## How It Works (from AI_Donation_Matcher_FINAL_v4_.md)

1. Donor opens the discovery page. Browser requests GPS location permission.
2. Map + list view populate with verified NGOs within the default 5 km radius.
3. Each pin is colour-coded by need category. The list is sorted by distance (urgency as secondary sort).
4. Donor can filter by: **search bar** (NGO name — overrides radius), **category dropdown**, **radius slider** (1–50 km).
5. Both category + search apply simultaneously to the same backend query.
6. **Auto-expand:** If no NGOs found within set radius, frontend expands to show all verified NGOs nationwide with banner: _"No NGOs found nearby. Showing all available NGOs."_

---

## Additions to `controller/NgoController.java`

Add these to the NgoController created in Feature 3 (NGO Profile):

```java
// ─── PUBLIC: Discovery ───────────────────────────────────────────────────

/**
 * GET /api/ngos?lat=&lng=&radius=&category=&search=
 *
 * Returns verified, profile-complete NGOs with their top active need.
 * If lat/lng omitted → returns all live NGOs (auto-expand fallback).
 * If search provided → name filter overrides radius.
 * Default radius: 50km.
 */
@GetMapping("/api/ngos")
public ResponseEntity<List<NgoDiscoveryDTO>> discoverNgos(
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) Double radius,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ngoService.discoverNgos(lat, lng, radius, category, search));
}

/**
 * GET /api/ngos/{id}
 *
 * Returns full NGO entity for the public profile page.
 */
@GetMapping("/api/ngos/{id}")
public ResponseEntity<Ngo> getNgoById(@PathVariable Long id) {
    return ResponseEntity.ok(ngoService.getNgoById(id));
}
```

**Additional imports needed:**
```java
import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
import com.aidonormatcher.backend.entity.Ngo;
import java.util.List;
```

---

## Actual Service Method Signatures

| Controller Call | Service Method | Returns |
|----------------|---------------|---------|
| `discoverNgos(lat, lng, radius, category, search)` | `ngoService.discoverNgos(Double, Double, Double, String, String)` | `List<NgoDiscoveryDTO>` |
| `getNgoById(id)` | `ngoService.getNgoById(Long)` | `Ngo` |

### `NgoService.discoverNgos` Logic:
1. If `lat`/`lng` are null → calls `ngoRepository.findAllLive(category, search)` → maps each Ngo to `NgoDiscoveryDTO` with `distanceKm = 0.0`
2. If `lat`/`lng` provided → calls `ngoRepository.findNearby(lat, lng, radius ?: 50.0, category, search)` → maps `Object[]` rows to `NgoDiscoveryDTO`
3. For each NGO, fetches top active need via `needRepository.findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(ngo, [OPEN, PARTIALLY_PLEDGED])`
4. Builds `NgoDiscoveryDTO` with NGO fields + top need info (item, quantity remaining, urgency, category)

### `NgoDiscoveryDTO` (already exists):
```java
public record NgoDiscoveryDTO(
    Long id, String name, double distanceKm,
    int trustScore, String trustTier,
    String topNeedItem, int topNeedQuantityRemaining,
    String topNeedUrgency, String topNeedCategory,
    Double lat, Double lng, String photoUrl
) {}
```

---

## Repository Queries (already exist)

**`NgoRepository.findNearby`** — Native Haversine query:
- Filters: `status = 'APPROVED'`, `profile_complete = true`, optional `category_of_work`, optional `name LIKE search`
- Returns `List<Object[]>` — `[Ngo, distanceKm]`

**`NgoRepository.findAllLive`** — Fallback (no radius):
- Same filters minus distance, ordered by `trust_score DESC`
- Returns `List<Ngo>`

---

## SecurityConfig Rules (already in place)

```java
.requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
```

No changes needed — GET on `/api/ngos` and `/api/ngos/{id}` are already public.

---

## Pin Popup Data (frontend consumes NgoDiscoveryDTO)

Each map pin popup shows:
- NGO name + photo thumbnail
- Trust score tier label
- Distance from donor
- Category colour icon
- Top 1 active need with remaining quantity

Donor clicks **"View Full Profile"** → navigates to `GET /api/ngos/{id}` which returns the full Ngo entity with all fields.

---

## Testing Checklist

- [ ] `GET /api/ngos?lat=12.97&lng=77.59&radius=25` → returns NGOs within 25km
- [ ] `GET /api/ngos?lat=12.97&lng=77.59&radius=25&category=FOOD` → filtered by category
- [ ] `GET /api/ngos?search=RedCross` → name filter, ignores radius
- [ ] `GET /api/ngos` (no params) → returns all live NGOs (auto-expand fallback)
- [ ] Response includes `topNeedItem`, `topNeedQuantityRemaining`, `topNeedUrgency`
- [ ] NGOs with `status != APPROVED` excluded
- [ ] NGOs with `profileComplete = false` excluded
- [ ] `GET /api/ngos/{id}` → returns full Ngo entity
- [ ] `GET /api/ngos/999` (invalid) → 400 "NGO not found."
- [ ] No JWT required for any of these endpoints
