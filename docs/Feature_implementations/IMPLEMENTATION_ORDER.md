# Implementation Order — Controller Layer

> **Context:** All services, entities, DTOs, repositories, enums, and config are complete.  
> **What's missing:** REST controllers to expose services to the frontend.  
> **Frontend expects:** `Authorization: Bearer <token>` on all authenticated calls via Axios interceptor.

---

## API Reference (from FEATURES.md + BACKEND.md)

| # | Method | Endpoint | Auth | Role | Feature |
|---|--------|----------|------|------|---------|
| 1 | `POST` | `/api/auth/register` | No | — | 1.1 |
| 2 | `GET` | `/api/auth/verify?token=` | No | — | 1.2 |
| 3 | `POST` | `/api/auth/login` | No | — | 1.3 |
| 4 | `POST` | `/api/auth/resend-verification` | No | — | 1.4 ⬜ |
| 5 | `GET` | `/api/ngos?lat=&lng=&radius=&category=&search=` | No | — | 2.1 |
| 6 | `GET` | `/api/ngos/{id}` | No | — | 2.2 |
| 7 | `POST` | `/api/ngos/{id}/report` | Yes | Any | 2.3 |
| 8 | `GET` | `/api/ngo/my/profile` | Yes | NGO | 3.1 |
| 9 | `PUT` | `/api/ngo/my/profile` | Yes | NGO | 3.2 |
| 10 | `POST` | `/api/ngo/my/photo` | Yes | NGO | 3.3 |
| 11 | `GET` | `/api/ngo/my/needs` | Yes | NGO | 4.1 |
| 12 | `POST` | `/api/needs` | Yes | NGO | 4.2 |
| 13 | `PUT` | `/api/needs/{id}` | Yes | NGO | 4.3 |
| 14 | `DELETE` | `/api/needs/{id}` | Yes | NGO | 4.4 |
| 15 | `PATCH` | `/api/needs/{id}/fulfill` | Yes | NGO | 4.5 |
| 16 | `POST` | `/api/pledges` | Yes | DONOR | 5.1 |
| 17 | `DELETE` | `/api/pledges/{id}` | Yes | DONOR | 5.2 |
| 18 | `GET` | `/api/pledges/active` | Yes | DONOR | 5.3 |
| 19 | `GET` | `/api/pledges/history` | Yes | DONOR | 5.4 |
| 20 | `GET` | `/api/admin/ngos/pending` | Yes | ADMIN | 6.1 |
| 21 | `POST` | `/api/admin/ngos/{id}/approve` | Yes | ADMIN | 6.2 |
| 22 | `POST` | `/api/admin/ngos/{id}/reject` | Yes | ADMIN | 6.3 |
| 23 | `POST` | `/api/admin/ngos/{id}/suspend` | Yes | ADMIN | 6.4 |
| 24 | `GET` | `/api/admin/reports` | Yes | ADMIN | 7.1 |
| 25 | `PUT` | `/api/admin/needs/{id}` | Yes | ADMIN | 7.2 |
| 26 | `DELETE` | `/api/admin/needs/{id}` | Yes | ADMIN | 7.3 |
| 27 | `GET` | `/api/admin/stats` | Yes | ADMIN | 7.4 |

---

## Build Order

> Follows the recommended order from FEATURES.md:  
> **1 → 3 → 4 → 2 → 5 → 6+7 → 1.4 → 7.4**  
> Controllers should be built in this sequence to enable end-to-end testing at each stage.

### Step 0 — Global Exception Handler (prerequisite)

| File | Type |
|------|------|
| `controller/GlobalExceptionHandler.java` | NEW |

Maps service-layer exceptions to proper HTTP status codes. All controllers depend on this.

**Guide:** `_00_Global_Exception_Handler.md`

---

### Step 1 — Authentication (unblocks everything)

| File | Type |
|------|------|
| `controller/AuthController.java` | NEW |

Endpoints: register, verify email, login. All public.  
Feature 1.4 (resend verification) is ⬜ — service method not yet implemented; add last.

**Guide:** `Feature_1_Authentication.md`  
**Test:** Register a donor → verify email → login → get JWT

---

### Step 2 — NGO Profile Management (NGOs go live)

| File | Type |
|------|------|
| `controller/NgoController.java` | NEW |

Endpoints: get/update own profile, upload photo.  
NGOs must complete profile (all required fields + geocoded address) before appearing on the discovery map.

**Guide:** `Feature_3_NGO_Profile.md`  
**Test:** Login as NGO → complete profile → verify `profileComplete=true` → address geocoded to lat/lng

---

### Step 3 — Needs Management (NGOs post needs)

| File | Type |
|------|------|
| `controller/NeedController.java` | NEW |
| `config/SecurityConfig.java` | MODIFY — add role rules for `/api/needs/**` |

Endpoints: list own needs, create/edit/delete/fulfill needs.

**Guide:** `Feature_4_Needs_Management.md`  
**Test:** Login as NGO → create 5 needs → verify 6th is rejected → fulfill one

---

### Step 4 — NGO Public Discovery (donor-facing map feed)

| File | Type |
|------|------|
| `controller/NgoController.java` | MODIFY — add discovery + public view endpoints |

Endpoints: `GET /api/ngos` (Haversine discovery with radius, category, search filters), `GET /api/ngos/{id}` (public profile).  
Uses existing `NgoService.discoverNgos()`, `NgoRepository.findNearby()`, and `NgoDiscoveryDTO`.

**Guide:** `Feature_2_NGO_Discovery.md`  
**Test:** Seed DB with approved, profile-complete NGOs with active needs → call `GET /api/ngos?lat=12.97&lng=77.59&radius=25`

---

### Step 5 — Pledge System (core donation loop)

| File | Type |
|------|------|
| `controller/PledgeController.java` | NEW |

Endpoints: create pledge, cancel, get active (`/api/pledges/active`), get history.

**Guide:** `Feature_5_Pledge_System.md`  
**Test:** Login as donor → pledge → verify quantity updates → cancel → verify quantity restored

---

### Step 6 + 7 — Admin Verification + Moderation

| File | Type |
|------|------|
| `controller/AdminController.java` | NEW |

Verification: pending NGOs, approve (POST), reject with reason (POST), suspend with cascade (POST).  
Moderation: reports, edit/remove need, stats dashboard.

**Guide:** `Feature_6_Admin_Verification.md` + `Feature_7_Admin_Moderation.md`  
**Test:** Login as admin → approve NGO → verify status + trust recalc → `GET /api/admin/stats`

---

### Step 8 — Report System

| File | Type |
|------|------|
| `controller/NgoController.java` | MODIFY — add report endpoint |
| `config/SecurityConfig.java` | MODIFY — add auth rule for POST on `/api/ngos/*/report` |

Endpoint: `POST /api/ngos/{id}/report`. Any authenticated user.

**Guide:** `Feature_8_Report_System.md`  
**Test:** Submit 3 reports for same NGO → verify admin alert email triggered

---

### Nice-to-have (add last)

| # | Feature | Status | Notes |
|---|---------|--------|-------|
| 1.4 | `POST /api/auth/resend-verification` | ⬜ | Service method not yet implemented |

---

## Files Created / Modified (Summary)

### New Files (6)

| File | Created In |
|------|-----------|
| `controller/GlobalExceptionHandler.java` | Step 0 |
| `controller/AuthController.java` | Step 1 |
| `controller/NgoController.java` | Step 2 (+ modified in Steps 4, 8) |
| `controller/NeedController.java` | Step 3 |
| `controller/PledgeController.java` | Step 5 |
| `controller/AdminController.java` | Step 6 |

### Modified Files (1)

| File | Steps |
|------|-------|
| `config/SecurityConfig.java` | Steps 3, 8 — add rules for `/api/needs/**` and `/api/ngos/*/report` |

---

## Important: Service Method Signatures

Controllers must match the **actual** service method signatures. These use **user/entity IDs**, not emails:

| Service Method | Parameters | Returns |
|---|---|---|
| `AuthService.register(req)` | `RegisterRequest` | `void` |
| `AuthService.verifyEmail(token)` | `String` | `void` |
| `AuthService.login(req)` | `LoginRequest` | `LoginResponse` |
| `NgoService.getMyProfile(email)` | `String` | `Ngo` |
| `NgoService.updateProfile(email, req)` | `String`, `NgoProfileRequest` | `Ngo` |
| `NgoService.updatePhotoUrl(email, url)` | `String`, `String` | `void` |
| `NgoService.getNgoById(id)` | `Long` | `Ngo` |
| `NgoService.discoverNgos(lat, lng, radius, category, search)` | `Double×2`, `Double`, `String×2` | `List<NgoDiscoveryDTO>` |
| `NeedService.createNeed(req, ngoId)` | `NeedRequest`, `Long` | `Need` |
| `NeedService.updateNeed(needId, req, ngoUserId)` | `Long`, `NeedRequest`, `Long` | `Need` |
| `NeedService.deleteNeed(needId, ngoUserId)` | `Long`, `Long` | `void` |
| `NeedService.fulfillNeed(needId, ngoUserId)` | `Long`, `Long` | `void` |
| `NeedService.getNeedsByNgo(ngo)` | `Ngo` | `List<Need>` |
| `PledgeService.createPledge(req, donorId)` | `PledgeRequest`, `Long` | `PledgeResponse` |
| `PledgeService.cancelPledge(pledgeId, donorId)` | `Long`, `Long` | `void` |
| `PledgeService.getActivePledges(donorId)` | `Long` | `List<Pledge>` |
| `PledgeService.getPledgeHistory(donorId)` | `Long` | `List<Pledge>` |
| `AdminService.approveNgo(ngoId)` | `Long` | `void` |
| `AdminService.rejectNgo(ngoId, reason)` | `Long`, `String` | `void` |
| `AdminService.suspendNgo(ngoId)` | `Long` | `void` |
| `AdminService.removeNeed(needId)` | `Long` | `void` |
| `AdminService.editNeed(needId, req)` | `Long`, `NeedRequest` | `Need` |
| `AdminService.getReports()` | — | `List<Report>` |
| `AdminService.getStats()` | — | `Map<String, Object>` |
| `ReportService.submitReport(ngoId, reason, reporterUserId)` | `Long`, `String`, `Long` | `void` |

> Controllers extract user IDs from `@AuthenticationPrincipal User user` → `user.getId()`.  
> NGO-specific controllers resolve the `Ngo` entity via `NgoRepository.findByUser(user)` or `findByUserId(user.getId())` when service methods need `ngoId` or `Ngo`.
