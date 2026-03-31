# Feature 4: Needs Management

Last updated: 2026-03-31

This feature area is implemented.

---

## Endpoints

- `GET /api/ngo/my/needs`
- `POST /api/needs`
- `PUT /api/needs/{id}`
- `DELETE /api/needs/{id}`
- `PATCH /api/needs/{id}/fulfill`

---

## Current rules

- NGOs can have at most 5 active needs
- only the owning NGO can modify its needs
- need update/delete is blocked once quantity has been pledged
- fulfillment is based on `quantity_received`, not only `quantity_pledged`

---

## State logic

- `OPEN` when no quantity is pledged
- `PARTIALLY_PLEDGED` when pledged quantity is between 1 and required
- `FULLY_PLEDGED` when pledged quantity equals required
- `FULFILLED` when received quantity reaches required
- `EXPIRED` when the daily job closes the need

---

## Files of interest

- `controller/NeedController.java`
- `service/NeedService.java`
- `entity/Need.java`
