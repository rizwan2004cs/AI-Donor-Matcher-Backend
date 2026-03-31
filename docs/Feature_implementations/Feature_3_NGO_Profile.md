# Feature 3: NGO Profile

Last updated: 2026-03-31

This feature area is implemented.

---

## Endpoints

- `GET /api/ngo/my/profile`
- `PUT /api/ngo/my/profile`
- `POST /api/ngo/my/photo`

---

## Current behavior

- NGOs complete their public profile after registration
- address save triggers geocoding through Nominatim
- coordinates are stored on the NGO record
- profile completeness is recalculated on save
- trust score is recalculated after profile updates
- photo uploads go to Cloudinary

---

## Profile completion inputs

- `name`
- `address`
- `contactEmail`
- `contactPhone`
- `description`
- `categoryOfWork`

The description must meet the minimum length rule enforced by the current service logic.

---

## Files of interest

- `controller/NgoController.java`
- `service/NgoService.java`
- `service/GeocodingService.java`
- `service/TrustScoreService.java`
- `service/CloudinaryService.java`
