# Feature Status

Last updated: 2026-03-31

This file tracks the current implemented behavior of the backend and the intended frontend contract around it.

---

## 1. Authentication

### Active flow

- Firebase Email/Password on the frontend
- backend verification through:
  - `POST /api/auth/firebase/register`
  - `POST /api/auth/firebase/login`

### Status

- Firebase donor registration: implemented
- Firebase NGO registration with multipart document upload: implemented
- Firebase login and local-role resolution: implemented
- legacy password and OTP endpoints: still present, not primary flow

---

## 2. NGO Discovery

- `GET /api/ngos`: implemented
- `GET /api/ngos/{id}`: implemented
- approved NGOs are discoverable without requiring `profile_complete=true`
- nearby discovery uses Haversine distance when `lat/lng` are provided
- fallback discovery returns all approved NGOs

---

## 3. NGO Profile

- `GET /api/ngo/my/profile`: implemented
- `PUT /api/ngo/my/profile`: implemented
- `POST /api/ngo/my/photo`: implemented
- address save triggers geocoding: implemented
- trust score recalculation after profile updates: implemented

---

## 4. NGO Needs

- `GET /api/ngo/my/needs`: implemented
- `POST /api/needs`: implemented
- `PUT /api/needs/{id}`: implemented
- `DELETE /api/needs/{id}`: implemented
- `PATCH /api/needs/{id}/fulfill`: implemented

Rules:

- max 5 active needs per NGO
- edit/delete blocked once quantity is pledged
- fulfillment depends on received quantity reaching the target

---

## 5. Donor Pledges

- `POST /api/pledges`: implemented
- `GET /api/pledges/{id}`: implemented
- `DELETE /api/pledges/{id}`: implemented
- `GET /api/pledges/active`: implemented
- `GET /api/pledges/history`: implemented

Rules:

- donor-only
- no email-verification gate for pledge creation
- 48-hour expiry window
- pessimistic database lock on pledge creation

---

## 6. NGO Incoming Pledges

- `GET /api/ngo/my/pledges`: implemented
- `PATCH /api/ngo/my/pledges/{pledgeId}/receive`: implemented

Behavior:

- NGOs see active and fulfilled incoming pledges
- receiving a pledge increases `quantity_received`
- need state recalculates after each receipt

---

## 7. Admin Verification and Moderation

- `GET /api/admin/ngos/pending`: implemented
- `GET /api/admin/ngos`: implemented
- `GET /api/admin/ngos/{id}/needs`: implemented
- `POST /api/admin/ngos/{id}/approve`: implemented
- `POST /api/admin/ngos/{id}/reject`: implemented
- `POST /api/admin/ngos/{id}/suspend`: implemented
- `GET /api/admin/reports`: implemented
- `PUT /api/admin/needs/{id}`: implemented
- `DELETE /api/admin/needs/{id}`: implemented
- `GET /api/admin/stats`: implemented

---

## 8. Reports

- `POST /api/ngos/{id}/report`: implemented
- admin report queue: implemented
- admin auto-flag email when reports reach 3 for the same NGO: implemented

---

## 9. Background Jobs

- hourly pledge expiry: implemented
- daily need expiry processing: implemented
- need expiry warning email: implemented

---

## 10. Deployment Readiness

- Render blueprint: ready
- Firebase Admin JSON deployment support: ready
- Neon PostgreSQL support: ready
- Vercel frontend integration contract: ready

See:

- [`AI_Donation_Matcher_FINAL_v4_.md`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/docs/AI_Donation_Matcher_FINAL_v4_.md)
- [`DEPLOYMENT_RENDER.md`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/docs/DEPLOYMENT_RENDER.md)
