# Feature 2: NGO Discovery

Last updated: 2026-03-31

This feature area is implemented and currently powers the donor discovery map and NGO detail page.

---

## Public endpoints

- `GET /api/ngos`
- `GET /api/ngos/{id}`
- `GET /api/needs/{id}`

---

## Discovery behavior

### Nearby mode

When `lat` and `lng` are provided, the backend:

- queries approved NGOs with non-null coordinates
- calculates distance using a native Haversine query
- filters by optional `radius`, `category`, and `search`
- returns `NgoDiscoveryDTO`

### Fallback mode

When `lat` and `lng` are missing, the backend:

- returns all approved NGOs
- sorts by trust score descending

---

## Important current rule

Approved NGOs are no longer filtered out for `profile_complete=false`.

That change was made so newly approved NGOs with valid coordinates still appear on the discovery map.

---

## Detail page behavior

`GET /api/ngos/{id}` currently returns:

- public NGO identity fields
- trust score and trust tier
- verification timestamp
- coordinates
- `activeNeeds`

The frontend still has placeholder rendering for fulfilled history, but the backend detail contract currently centers on active needs.

---

## Files of interest

- `controller/NgoController.java`
- `service/NgoService.java`
- `repository/NgoRepository.java`
- `dto/NgoDiscoveryDTO.java`
- `dto/NgoDetailResponse.java`
