# Frontend-Backend Agreement

Last updated: 2026-03-31

This is the current contract between the React frontend and the Spring Boot backend.

Source of truth order:

1. live backend implementation
2. Swagger / OpenAPI
3. this agreement

---

## 1. Base URLs

- Local backend: `http://localhost:8080`
- Local frontend: `http://localhost:5173`
- Production frontend: Vercel
- Production backend: Render

Frontend uses:

- `VITE_API_BASE_URL`

Backend uses:

- `APP_BASE_URL`
- `CORS_ALLOWED_ORIGIN_PATTERNS`

---

## 2. Active Authentication Contract

### The active auth flow is Firebase-first

Frontend must:

1. sign in or sign up through Firebase Web SDK
2. get the Firebase ID token
3. send that token as `Authorization: Bearer <id-token>` to the backend

### Active backend auth endpoints

- `POST /api/auth/firebase/register`
- `POST /api/auth/firebase/login`

### Frontend storage behavior

- frontend stores the bearer token in localStorage under `token`
- frontend stores the normalized user object in auth state
- frontend reuses the bearer token on protected API calls through the shared axios interceptor

### Current user response shape

```json
{
  "token": "<firebase-id-token>",
  "userId": 1,
  "fullName": "Alice Donor",
  "email": "alice@example.com",
  "role": "DONOR"
}
```

Important:

- frontend should not depend on `emailVerified`
- frontend should route using `role`
- NGO routing should still fetch `/api/ngo/my/profile` for completion and dashboard state

---

## 3. Dormant / Legacy Auth Endpoints

These endpoints still exist server-side but are not the primary frontend flow:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/resend-verification`
- `POST /api/auth/send-registration-otp`
- `POST /api/auth/send-otp`
- `POST /api/auth/verify-otp`
- `GET /api/auth/verify`

Frontend should not actively use the OTP endpoints in the deployed flow.

---

## 4. Error Shapes

### Validation errors

```json
{
  "error": "Validation failed.",
  "fieldErrors": {
    "email": "must not be blank"
  },
  "status": 400,
  "timestamp": "2026-03-31T18:00:00"
}
```

### Runtime errors

```json
{
  "error": "Need not found.",
  "status": 400,
  "timestamp": "2026-03-31T18:00:00"
}
```

### Auth / access errors

- `401` -> bad credentials or invalid bearer token
- `403` -> authenticated but wrong role

---

## 5. Public Contract

### `GET /api/ngos`

Supported query params:

- `lat`
- `lng`
- `radius`
- `category`
- `search`

Behavior:

- if `lat/lng` are present, backend returns nearby approved NGOs with distance
- if `lat/lng` are absent, backend returns all approved NGOs
- approved NGOs are not filtered out for `profile_complete=false`

### `GET /api/ngos/{id}`

Returns public NGO detail plus `activeNeeds`.

Current response includes:

- NGO identity and contact fields
- trust score and tier
- verified timestamp
- coordinates
- `activeNeeds`

### `GET /api/needs/{id}`

Returns a single public need detail record.

---

## 6. Donor Contract

### Protected donor endpoints

- `POST /api/ngos/{id}/report`
- `POST /api/pledges`
- `GET /api/pledges/{id}`
- `DELETE /api/pledges/{id}`
- `GET /api/pledges/active`
- `GET /api/pledges/history`

### Create pledge

Request:

```json
{
  "needId": 14,
  "quantity": 2
}
```

Response:

```json
{
  "pledgeId": 9,
  "ngoLat": 14.4426,
  "ngoLng": 79.9865,
  "ngoAddress": "Some NGO address",
  "ngoContactEmail": "ngo@example.com",
  "expiresAt": "2026-04-02T11:30:00"
}
```

Important:

- frontend must not block on `emailVerified`
- frontend should redirect to `/delivery/:pledgeId` using this response

### Pledge detail

`GET /api/pledges/{id}` is the refresh-safe payload for delivery and pledge detail pages.

### Active vs history

- `/api/pledges/active` -> active only
- `/api/pledges/history` -> all statuses for that donor

---

## 7. NGO Contract

### Protected NGO endpoints

- `GET /api/ngo/my/profile`
- `PUT /api/ngo/my/profile`
- `POST /api/ngo/my/photo`
- `GET /api/ngo/my/pledges`
- `PATCH /api/ngo/my/pledges/{pledgeId}/receive`
- `GET /api/ngo/my/needs`
- `POST /api/needs`
- `PUT /api/needs/{id}`
- `DELETE /api/needs/{id}`
- `PATCH /api/needs/{id}/fulfill`

### Profile update behavior

- backend geocodes address on save
- backend recalculates trust score on profile save
- invalid non-blank address can fail the save

### NGO photo upload

- `multipart/form-data`
- field name: `file`

Response:

```json
{
  "url": "https://res.cloudinary.com/..."
}
```

### Incoming pledges

`GET /api/ngo/my/pledges` returns incoming pledge rows for the NGO dashboard.

### Mark received

`PATCH /api/ngo/my/pledges/{pledgeId}/receive`

Behavior:

- only owner NGO can call it
- only `ACTIVE` pledge can be received
- received quantity updates the need

---

## 8. Needs Contract

### Create / update request body

```json
{
  "category": "EDUCATION",
  "itemName": "School Kits",
  "description": "Notebooks and stationery",
  "quantityRequired": 25,
  "urgency": "URGENT",
  "expiryDate": "2026-04-10"
}
```

### Need rules

- max 5 active needs per NGO
- update/delete blocked once quantity has been pledged
- manual fulfillment requires full received quantity

---

## 9. Admin Contract

### Protected admin endpoints

- `GET /api/admin/ngos/pending`
- `GET /api/admin/ngos`
- `GET /api/admin/ngos/{id}/needs`
- `POST /api/admin/ngos/{id}/approve`
- `POST /api/admin/ngos/{id}/reject`
- `POST /api/admin/ngos/{id}/suspend`
- `GET /api/admin/reports`
- `PUT /api/admin/needs/{id}`
- `DELETE /api/admin/needs/{id}`
- `GET /api/admin/stats`

### Reject body

```json
{
  "reason": "Missing supporting documents"
}
```

### Admin stats

Current stats object contains platform counts such as:

- `totalUsers`
- `totalNgos`
- `pendingNgos`
- `approvedNgos`
- `suspendedNgos`
- `totalNeeds`
- `activeNeeds`
- `totalPledges`
- `pledgesToday`
- `fulfillmentsThisMonth`
- `totalReports`

Frontend should treat this as a dynamic stats object and render only keys it needs.

---

## 10. Frontend Responsibilities

- use Firebase flows for `/login` and `/register`
- send bearer token on protected API requests
- do not use OTP endpoints in the active deployed UI
- do not gate pledge UI on `emailVerified`
- handle `204 No Content` correctly
- preserve form state on geocoding failure

---

## 11. Backend Responsibilities

- keep Swagger aligned with the implementation
- keep this agreement aligned with any contract change
- preserve route and field stability where possible
- document dormant legacy endpoints clearly if they remain in the codebase
