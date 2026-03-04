# Implementation Order — Controller Layer + Frontend API Alignment

> **Context:** All services, entities, DTOs, repositories, enums, and config are complete.  
> **What's missing:** REST controllers to expose services, plus one new endpoint (`GET /api/needs/nearby`) the frontend requires.  
> **Frontend expects:** `Authorization: Bearer <token>` on all authenticated calls via Axios interceptor.

---

## Frontend API Contract

| # | Method | Frontend Endpoint | Auth | Role |
|---|--------|-------------------|------|------|
| 1 | `POST` | `/api/auth/register` | No | — |
| 2 | `POST` | `/api/auth/login` | No | — |
| 3 | `GET` | `/api/auth/verify?token=` | No | — |
| 4 | `GET` | `/api/needs/nearby?lat=&lng=&radius=&category=` | No | — |
| 5 | `POST` | `/api/pledges` | Yes | DONOR |
| 6 | `DELETE` | `/api/pledges/{id}` | Yes | DONOR |
| 7 | `GET` | `/api/pledges/donor/active` | Yes | DONOR |
| 8 | `GET` | `/api/pledges/history` | Yes | DONOR |
| 9 | `GET` | `/api/ngo/my/profile` | Yes | NGO |
| 10 | `PUT` | `/api/ngo/my/profile` | Yes | NGO |
| 11 | `POST` | `/api/ngo/my/photo` | Yes | NGO |
| 12 | `GET` | `/api/ngo/my/needs` | Yes | NGO |
| 13 | `POST` | `/api/needs` | Yes | NGO |
| 14 | `PUT` | `/api/needs/{id}` | Yes | NGO |
| 15 | `DELETE` | `/api/needs/{id}` | Yes | NGO |
| 16 | `PATCH` | `/api/needs/{id}/fulfill` | Yes | NGO |
| 17 | `POST` | `/api/ngos/{id}/report` | Yes | Any |
| 18 | `GET` | `/api/ngos/{id}` | No | — |
| 19 | `GET` | `/api/admin/ngos/pending` | Yes | ADMIN |
| 20 | `GET` | `/api/admin/ngos` | Yes | ADMIN |
| 21 | `POST` | `/api/admin/ngos/{id}/approve` | Yes | ADMIN |
| 22 | `POST` | `/api/admin/ngos/{id}/reject` | Yes | ADMIN |
| 23 | `POST` | `/api/admin/ngos/{id}/suspend` | Yes | ADMIN |
| 24 | `GET` | `/api/admin/reports` | Yes | ADMIN |
| 25 | `PUT` | `/api/admin/needs/{id}` | Yes | ADMIN |
| 26 | `DELETE` | `/api/admin/needs/{id}` | Yes | ADMIN |
| 27 | `GET` | `/api/admin/stats` | Yes | ADMIN |

---

## Build Order (step-by-step)

Each step is independently deployable and testable. Build in this exact order.

### Step 0 — Global Exception Handler (prerequisite)

| File | Type |
|------|------|
| `controller/GlobalExceptionHandler.java` | NEW |

Maps service exceptions to proper HTTP status codes. All controllers depend on this.

**Guide:** `Feature_implementations/_00_Global_Exception_Handler.md`

---

### Step 1 — Authentication (unblocks everything)

| File | Type |
|------|------|
| `controller/AuthController.java` | NEW |

Endpoints: register, login, verify email. All public.

**Guide:** `Feature_implementations/Feature_1_Authentication.md`  
**Test:** Register a donor → verify email → login → get JWT

---

### Step 2 — Nearby Needs Discovery (frontend's map feed)

| File | Type |
|------|------|
| `dto/NearbyNeedDTO.java` | NEW |
| `repository/NeedRepository.java` | MODIFY — add Haversine query |
| `service/NeedService.java` | MODIFY — add `findNearbyNeeds()` |
| `controller/NeedController.java` | NEW |
| `config/SecurityConfig.java` | MODIFY — add permitAll for `/api/needs/nearby` |

This is the **biggest change** — the frontend expects need-centric discovery (`/api/needs/nearby`) but the backend only has NGO-centric discovery. Requires a new native Haversine query joining `needs → ngos`.

**Guide:** `Feature_implementations/Feature_2_Nearby_Needs.md`  
**Test:** Seed DB with NGOs (with lat/lng) + active needs → call `/api/needs/nearby?lat=...&lng=...&radius=50`

---

### Step 3 — NGO Profile Management (NGOs go live)

| File | Type |
|------|------|
| `controller/NgoController.java` | NEW |

Endpoints: get/update profile, upload photo. Plus public `GET /api/ngos/{id}`.

**Guide:** `Feature_implementations/Feature_3_NGO_Profile.md`  
**Test:** Login as NGO → complete profile → verify `profileComplete=true`

---

### Step 4 — Needs Management (NGOs post needs)

| File | Type |
|------|------|
| `controller/NeedController.java` | MODIFY — add NGO endpoints |

Endpoints: list own needs, create/edit/delete/fulfill needs. Adds to NeedController from Step 2.

**Guide:** `Feature_implementations/Feature_4_Needs_Management.md`  
**Test:** Login as NGO → create 5 needs → verify 6th is rejected → fulfill one

---

### Step 5 — Pledge System (core donation loop)

| File | Type |
|------|------|
| `controller/PledgeController.java` | NEW |

Endpoints: create pledge, cancel, get active (`/api/pledges/donor/active`), get history.

**Guide:** `Feature_implementations/Feature_5_Pledge_System.md`  
**Test:** Login as donor → pledge → verify quantity updates → cancel → verify restored

---

### Step 6 — Admin Verification

| File | Type |
|------|------|
| `controller/AdminController.java` | NEW |

Endpoints: pending NGOs, approve, reject, suspend (cascade).

**Guide:** `Feature_implementations/Feature_6_Admin_Verification.md`  
**Test:** Login as admin → approve NGO → verify status change + trust recalc

---

### Step 7 — Admin Moderation + Stats

| File | Type |
|------|------|
| `controller/AdminController.java` | MODIFY — add moderation endpoints |

Endpoints: reports queue, edit/remove need, stats dashboard.

**Guide:** `Feature_implementations/Feature_7_Admin_Moderation.md`  
**Test:** `GET /api/admin/stats` → verify all 6 keys present

---

### Step 8 — Report System

| File | Type |
|------|------|
| `controller/NgoController.java` | MODIFY — add report endpoint |
| `config/SecurityConfig.java` | MODIFY — add auth rule for report |

Endpoint: `POST /api/ngos/{id}/report`. Any authenticated user.

**Guide:** `Feature_implementations/Feature_8_Report_System.md`  
**Test:** Submit 3 reports → verify admin alert email triggered

---

## Files Created / Modified (Summary)

### New Files (7)

| File | Created In |
|------|-----------|
| `controller/GlobalExceptionHandler.java` | Step 0 |
| `controller/AuthController.java` | Step 1 |
| `dto/NearbyNeedDTO.java` | Step 2 |
| `controller/NeedController.java` | Step 2 |
| `controller/NgoController.java` | Step 3 |
| `controller/PledgeController.java` | Step 5 |
| `controller/AdminController.java` | Step 6 |

### Modified Files (3)

| File | Steps |
|------|-------|
| `repository/NeedRepository.java` | Step 2 — add Haversine query |
| `service/NeedService.java` | Step 2 — add `findNearbyNeeds()` |
| `config/SecurityConfig.java` | Steps 2, 8 — add permit rules |

---

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| `GET /api/needs/nearby` (need-centric) instead of `GET /api/ngos` (NGO-centric) | Frontend expects needs as first-class map objects |
| `GET /api/pledges/donor/active` instead of `/api/pledges/active` | Matches exact frontend Axios call |
| `POST /api/ngos/{id}/report` open to any authenticated role | Donors and admins should both be able to report |
| Global exception handler as Step 0 | All controllers need consistent error responses |
