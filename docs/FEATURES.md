# Feature List — AI Donor Matcher Backend

All service-layer logic is implemented. The features below each require a **REST controller** (and any supporting wiring) to expose the service to the frontend.

---

## Status Key

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented (service + controller) |
| 🔧 | Service exists — controller needed |
| ⬜ | Not yet implemented |

---

## Feature Areas

### 1. Authentication (`AuthController`)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 1.1 | Register donor or NGO (with optional document upload) | `POST /api/auth/register` | ✅ |
| 1.2 | Verify email via token link | `GET /api/auth/verify?token=` | ✅ |
| 1.3 | Login and receive JWT | `POST /api/auth/login` | ✅ |
| 1.4 | Resend verification email | `POST /api/auth/resend-verification` | ✅ |

---

### 2. NGO Public Discovery (`NgoController` — public endpoints)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 2.1 | Discover nearby NGOs via Haversine query (radius, category, search filters) | `GET /api/ngos` | 🔧 |
| 2.2 | Get full public NGO profile by ID | `GET /api/ngos/{id}` | 🔧 |
| 2.3 | Donor submits a report against an NGO | `POST /api/ngos/{id}/report` | 🔧 |

---

### 3. NGO Profile Management (`NgoController` — NGO-role endpoints)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 3.1 | Get own NGO profile | `GET /api/ngo/my/profile` | 🔧 |
| 3.2 | Update own NGO profile (name, address, description, phone, category) | `PUT /api/ngo/my/profile` | 🔧 |
| 3.3 | Upload NGO profile photo to Cloudinary | `POST /api/ngo/my/photo` | 🔧 |

---

### 4. NGO Needs Management (`NeedController` — NGO-role endpoints)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 4.1 | List this NGO's own needs | `GET /api/ngo/my/needs` | 🔧 |
| 4.2 | Create a new resource need (max 5 active) | `POST /api/needs` | 🔧 |
| 4.3 | Edit a need (blocked if any pledge exists) | `PUT /api/needs/{id}` | 🔧 |
| 4.4 | Delete a need (blocked if any pledge exists) | `DELETE /api/needs/{id}` | 🔧 |
| 4.5 | Mark a need as fulfilled | `PATCH /api/needs/{id}/fulfill` | 🔧 |

---

### 5. Pledge System (`PledgeController` — donor-role endpoints)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 5.1 | Create a pledge against a need (pessimistic-locked, split-pledge) | `POST /api/pledges` | 🔧 |
| 5.2 | Cancel an active pledge | `DELETE /api/pledges/{id}` | 🔧 |
| 5.3 | Get all active pledges for the logged-in donor | `GET /api/pledges/active` | 🔧 |
| 5.4 | Get full pledge donation history for the logged-in donor | `GET /api/pledges/history` | 🔧 |

---

### 6. Admin — NGO Verification (`AdminController`)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 6.1 | List all NGOs with PENDING status | `GET /api/admin/ngos/pending` | 🔧 |
| 6.2 | Approve an NGO application | `POST /api/admin/ngos/{id}/approve` | 🔧 |
| 6.3 | Reject an NGO application with reason | `POST /api/admin/ngos/{id}/reject` | 🔧 |
| 6.4 | Suspend an NGO (atomic cascade — closes needs, cancels pledges, notifies donors) | `POST /api/admin/ngos/{id}/suspend` | 🔧 |

---

### 7. Admin — Content Moderation (`AdminController`)

| # | Feature | Endpoint | Status |
|---|---------|----------|--------|
| 7.1 | View all donor-submitted reports | `GET /api/admin/reports` | 🔧 |
| 7.2 | Admin edits any NGO's need | `PUT /api/admin/needs/{id}` | 🔧 |
| 7.3 | Admin removes a need (cascade: cancel pledges, notify donors) | `DELETE /api/admin/needs/{id}` | 🔧 |
| 7.4 | Platform stats overview (NGO count, active needs, pledges today, fulfillments this month) | `GET /api/admin/stats` | ⬜ |

---

### 8. Scheduled Background Jobs (no HTTP endpoint — internal)

| # | Feature | Class | Status |
|---|---------|-------|--------|
| 8.1 | Auto-expire pledges older than 48 hours (runs every hour) | `ScheduledJobService#expireOldPledges` | ✅ |
| 8.2 | Close needs whose expiry date has passed (runs daily at midnight) | `ScheduledJobService#processNeedExpiry` | ✅ |
| 8.3 | Warn NGOs of needs expiring within 3 days | `ScheduledJobService#processNeedExpiry` | ✅ |
| 8.4 | Daily trust score recalculation (recency penalty) | `TrustScoreService#recalculate` | ✅ |

---

### 9. Email Notifications (triggered by services — no HTTP endpoint)

| # | Trigger | Status |
|---|---------|--------|
| 9.1 | Email verification on registration | ✅ |
| 9.2 | Admin notification on new NGO application | ✅ |
| 9.3 | NGO approval email | ✅ |
| 9.4 | NGO rejection email with reason | ✅ |
| 9.5 | Pledge confirmation email to donor (includes NGO address + contact) | ✅ |
| 9.6 | Pledge notification email to NGO | ✅ |
| 9.7 | Pledge cancelled by donor — notify NGO | ✅ |
| 9.8 | Pledge auto-expired — notify donor | ✅ |
| 9.9 | Admin removes need — notify affected donors | ✅ |
| 9.10 | NGO suspended — notify affected donors | ✅ |
| 9.11 | Need fulfillment thank-you email to donor | ✅ |
| 9.12 | Need expiry warning to NGO (3 days before) | ✅ |

---

## Build Order (Recommended)

Controllers should be built in this sequence to enable end-to-end testing at each stage:

1. **Feature 1** — Auth (register, verify, login) — unblocks all other features
2. **Feature 3** — NGO profile management — required before NGOs can go live
3. **Feature 4** — Needs management — required before pledges work
4. **Feature 2** — Public NGO discovery — donor-facing map feed
5. **Feature 5** — Pledge system — core donation loop
6. **Feature 6 + 7** — Admin verification and moderation
7. **Feature 1.4** — Resend verification email (nice-to-have, add last)
8. **Feature 7.4** — Admin stats dashboard (nice-to-have, add last)
