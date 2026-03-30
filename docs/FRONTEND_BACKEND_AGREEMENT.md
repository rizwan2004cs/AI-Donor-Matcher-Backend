# Frontend-Backend Agreement

Last updated: 2026-03-30

This document is the working contract between the frontend and backend for the AI Donor Matcher project.

Use this together with Swagger UI:
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

If this document and the frontend implementation differ, this document and Swagger should be treated as the source of truth.

## 1. Base Rules

### Base URL
- Local backend: `http://localhost:8080`
- Production/staging: use the deployed backend domain with the same routes

### Content type
- Default request body: `application/json`
- NGO registration with document upload: `multipart/form-data`
- NGO photo upload: `multipart/form-data`

### Authentication header
For protected endpoints, frontend must send:

```http
Authorization: Bearer <jwt-token>
```

### JWT storage
- Frontend stores the token returned by login or successful registration
- Frontend includes the token on protected requests only
- Frontend should clear the token on logout or `401`

## 2. Registration Agreement

The active registration flow is OTP-first.

### Required frontend flow
1. User enters registration details on the registration page.
2. Frontend calls `POST /api/auth/send-registration-otp` with the email.
3. Frontend redirects user to the OTP page.
4. User enters the OTP.
5. Frontend submits `POST /api/auth/register` with the full registration payload including `otp`.
6. Backend creates the account only if the OTP is valid and returns the authenticated login payload immediately.

### Important behavior
- No account is created before OTP-backed registration succeeds.
- Successful registration creates the user as already verified.
- Successful registration also auto-logs in both donors and NGOs.
- Frontend must not use link-based verification for new registration.
- `GET /api/auth/verify` exists only as a legacy endpoint and is not part of the active frontend flow.
- NGO users may still need profile completion or admin approval after auto-login, so frontend should route them using `role` first and then fetch `GET /api/ngo/my/profile` for completion/approval-aware gating.

## 3. Standard Response Agreement

### Success shapes
Common success responses are:

```json
{ "message": "..." }
```

or typed objects such as login, pledge, NGO, or need data.

### Validation error shape
When request validation fails, backend returns:

```json
{
  "error": "Validation failed.",
  "fieldErrors": {
    "email": "must not be blank",
    "otp": "must not be blank"
  },
  "status": 400,
  "timestamp": "2026-03-30T17:00:00"
}
```

### Runtime error shape
For business-rule errors, backend returns:

```json
{
  "error": "Email already registered.",
  "status": 400,
  "timestamp": "2026-03-30T17:00:00"
}
```

### Security error shape
- `401` means invalid login or missing/invalid auth state
- `403` means authenticated but not allowed

Example:

```json
{
  "error": "Access denied.",
  "status": 403,
  "timestamp": "2026-03-30T17:00:00"
}
```

## 4. Auth Contract

### Send registration OTP
`POST /api/auth/send-registration-otp`

Request:
```json
{
  "email": "user@example.com"
}
```

Response:
```json
{
  "message": "Verification code sent to your email"
}
```

### Register donor or NGO
`POST /api/auth/register`

JSON or multipart payload fields:

```json
{
  "fullName": "Helping Hands Foundation",
  "email": "ngo@example.com",
  "password": "StrongPassword123",
  "role": "NGO",
  "location": "Colombo",
  "otp": "123456"
}
```

Multipart note:
- For NGO registration, frontend may attach `documents` as the file part.

Success response:
```json
{
  "token": "jwt-token",
  "userId": 1,
  "fullName": "Helping Hands Foundation",
  "email": "ngo@example.com",
  "role": "NGO",
  "emailVerified": true
}
```

Frontend should treat this response exactly like the login response and persist the token immediately.

### Login
`POST /api/auth/login`

Request:
```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "token": "jwt-token",
  "userId": 1,
  "fullName": "Alice Donor",
  "email": "alice@example.com",
  "role": "DONOR",
  "emailVerified": true
}
```

Frontend should use `role` to route users to the correct dashboard.
If the user is an NGO and the app needs completion gating, frontend must fetch `GET /api/ngo/my/profile` after login because login does not include `profileComplete`.
The same post-login rule applies after successful NGO registration because registration now auto-logs in immediately.

### Resend verification OTP
`POST /api/auth/resend-verification`

Request:
```json
{
  "email": "user@example.com"
}
```

Response:
```json
{
  "message": "Verification code sent to your email."
}
```

### Send OTP to an existing email
`POST /api/auth/send-otp`

Request:
```json
{
  "email": "user@example.com"
}
```

Response:
```json
{
  "message": "Verification code sent"
}
```

### Verify OTP
`POST /api/auth/verify-otp`

Request:
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

Response:
```json
{
  "message": "Email verified"
}
```

## 5. Role Access Agreement

### Public
- `POST /api/auth/**`
- `GET /api/ngos`
- `GET /api/ngos/{id}`

### NGO-only
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

### Donor-only
- `POST /api/ngos/{id}/report`
- `POST /api/pledges`
- `DELETE /api/pledges/{id}`
- `GET /api/pledges/active`
- `GET /api/pledges/history`

### Admin-only
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

## 6. NGO Contract

### Get own NGO profile
`GET /api/ngo/my/profile`

Frontend should treat this as the editable NGO profile source.
This response is also the source of truth for NGO trust/completion fields such as `trustScore`, `trustTier`, and `profileComplete`.

### Update own NGO profile
`PUT /api/ngo/my/profile`

Request shape:
```json
{
  "name": "Helping Hands",
  "address": "123 Main Street, Colombo",
  "contactEmail": "contact@helpinghands.org",
  "contactPhone": "+94 77 123 4567",
  "description": "Supporting families with essential supplies.",
  "categoryOfWork": "FOOD"
}
```

Address geocoding behavior:
- backend uses OpenStreetMap Nominatim for NGO address geocoding
- when `address` is saved, backend resolves and stores `lat` and `lng`
- backend tries the full address first and then broader comma-separated area fallbacks if the full address is too specific or not indexed
- example fallback path: `Door No 12, Mulapet, Nellore` -> `Mulapet, Nellore` -> `Nellore`
- if all fallback queries fail or the provider is unavailable, backend rejects the save with a clear runtime error
- backend does not repeatedly retry the same candidate query in the request path
- current NGO signup does not collect the structured map address, so first geocoding usually happens on the first profile save

Frontend should expect `lat` and `lng` to remain available in the NGO responses already used by the app.

### Upload NGO photo
`POST /api/ngo/my/photo`

Request:
- `multipart/form-data`
- file field name: `file`

Response:
```json
{
  "url": "https://res.cloudinary.com/demo/image/upload/sample.jpg"
}
```

Frontend should immediately update displayed NGO photo with this URL.

### Discover NGOs
`GET /api/ngos`

Supported query params:
- `lat`
- `lng`
- `radius`
- `category`
- `search`

Frontend may call this with any combination of those filters.

### Public NGO detail
`GET /api/ngos/{id}`

Response shape:
```json
{
  "id": 5,
  "name": "Helping Hands",
  "address": "123 Main Street, Colombo",
  "contactEmail": "contact@helpinghands.org",
  "contactPhone": "+94 77 123 4567",
  "description": "Supporting families with essential supplies.",
  "categoryOfWork": "FOOD",
  "photoUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg",
  "trustScore": 82,
  "trustTier": "TRUSTED",
  "verifiedAt": "2026-03-30T10:15:00",
  "lat": 6.9271,
  "lng": 79.8612,
  "activeNeeds": [
    {
      "id": 12,
      "ngoId": 5,
      "ngoName": "Helping Hands",
      "ngoAddress": "123 Main Street, Colombo",
      "ngoPhotoUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg",
      "ngoTrustScore": 82,
      "ngoTrustTier": "TRUSTED",
      "category": "FOOD",
      "itemName": "Rice packs",
      "description": "5kg rice packs for 50 families",
      "quantityRequired": 50,
      "quantityPledged": 30,
      "quantityRemaining": 20,
      "urgency": "URGENT",
      "expiryDate": "2026-04-15",
      "status": "PARTIALLY_PLEDGED",
      "createdAt": "2026-03-30T10:15:00"
    }
  ]
}
```

### Report an NGO
`POST /api/ngos/{id}/report`

Request:
```json
{
  "reason": "Suspicious activity"
}
```

Response:
```json
{
  "message": "Report submitted."
}
```

### Incoming pledges for NGO dashboard
`GET /api/ngo/my/pledges`

Response item shape:
```json
{
  "pledgeId": 44,
  "needId": 12,
  "donorName": "Alice Donor",
  "donorEmail": "alice@example.com",
  "itemName": "Rice packs",
  "category": "FOOD",
  "quantity": 10,
  "status": "ACTIVE",
  "createdAt": "2026-03-30T10:15:00",
  "expiresAt": "2026-04-01T10:15:00",
  "needQuantityRequired": 22,
  "needQuantityPledged": 15,
  "needQuantityReceived": 15,
  "needQuantityRemaining": 7
}
```

Important behavior:
- this list now returns both incoming `ACTIVE` pledges and already confirmed `FULFILLED` pledges for the NGO
- `needQuantityRemaining` is based on quantities actually received by the NGO, not only donor commitments
- frontend should use `status` to decide whether a pledge still needs NGO confirmation

### Mark an incoming pledge as received
`PATCH /api/ngo/my/pledges/{pledgeId}/receive`

Response shape:
```json
{
  "pledgeId": 44,
  "needId": 12,
  "donorName": "Alice Donor",
  "donorEmail": "alice@example.com",
  "itemName": "Rice packs",
  "category": "FOOD",
  "quantity": 10,
  "status": "FULFILLED",
  "createdAt": "2026-03-30T10:15:00",
  "expiresAt": "2026-04-01T10:15:00",
  "needQuantityRequired": 22,
  "needQuantityPledged": 15,
  "needQuantityReceived": 15,
  "needQuantityRemaining": 7
}
```

Receipt behavior:
- only the owning NGO can call this endpoint
- only `ACTIVE` pledges can be marked as received
- marking a pledge as received moves it to `FULFILLED`
- the need is automatically recalculated after each received pledge
- once received quantity reaches the full required amount, the need becomes `FULFILLED`

## 7. Needs Contract

### Need detail
`GET /api/needs/{id}`

Response shape:
```json
{
  "id": 12,
  "ngoId": 5,
  "ngoName": "Helping Hands",
  "ngoAddress": "123 Main Street, Colombo",
  "ngoPhotoUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg",
  "ngoTrustScore": 82,
  "ngoTrustTier": "TRUSTED",
  "category": "FOOD",
  "itemName": "Rice packs",
  "description": "5kg rice packs for 50 families",
  "quantityRequired": 50,
  "quantityPledged": 30,
  "quantityRemaining": 20,
  "urgency": "HIGH",
  "expiryDate": "2026-04-15",
  "status": "PARTIALLY_PLEDGED",
  "createdAt": "2026-03-30T10:15:00"
}
```

### Create or update need
Used by:
- `POST /api/needs`
- `PUT /api/needs/{id}`

Request shape:
```json
{
  "category": "FOOD",
  "itemName": "Rice packs",
  "description": "5kg rice packs for 50 families",
  "quantityRequired": 50,
  "urgency": "HIGH",
  "expiryDate": "2026-04-15"
}
```

### Delete need
`DELETE /api/needs/{id}`

Response:
- `204 No Content`

### Fulfill need
`PATCH /api/needs/{id}/fulfill`

Response:
- `204 No Content`

Frontend must not expect a JSON body for `204` responses.

## 8. Pledge Contract

### Create pledge
`POST /api/pledges`

Request:
```json
{
  "needId": 12,
  "quantity": 10
}
```

Response:
```json
{
  "pledgeId": 44,
  "ngoLat": 6.9271,
  "ngoLng": 79.8612,
  "ngoAddress": "123 Main Street, Colombo",
  "ngoContactEmail": "contact@helpinghands.org",
  "expiresAt": "2026-03-31T10:30:00"
}
```

Frontend can use this response for:
- navigation or map handoff
- donation coordination details
- pledge-expiry display

### Cancel pledge
`DELETE /api/pledges/{id}`

Response:
- `204 No Content`

### Active pledges
`GET /api/pledges/active`

### Pledge history
`GET /api/pledges/history`

### Pledge detail
`GET /api/pledges/{id}`

Response shape:
```json
{
  "pledgeId": 44,
  "needId": 12,
  "ngoId": 5,
  "ngoName": "Helping Hands",
  "ngoPhotoUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg",
  "itemName": "Rice packs",
  "category": "FOOD",
  "quantity": 10,
  "status": "ACTIVE",
  "createdAt": "2026-03-30T10:15:00",
  "expiresAt": "2026-04-01T10:15:00",
  "ngoLat": 6.9271,
  "ngoLng": 79.8612,
  "ngoAddress": "123 Main Street, Colombo",
  "ngoContactEmail": "contact@helpinghands.org"
}
```

## 9. Admin Contract

### Reject NGO
`POST /api/admin/ngos/{id}/reject`

Request:
```json
{
  "reason": "Missing supporting documents"
}
```

Response:
```json
{
  "message": "NGO rejected."
}
```

### Approve NGO
`POST /api/admin/ngos/{id}/approve`

Response:
```json
{
  "message": "NGO approved."
}
```

### Pending NGO verification queue
`GET /api/admin/ngos/pending`

This currently returns the backend `Ngo` entity. For verification-queue UI, the confirmed fields available for document review include:
- `id`
- `name`
- `address`
- `contactEmail`
- `contactPhone`
- `description`
- `categoryOfWork`
- `photoUrl`
- `documentUrl`
- `status`
- `profileComplete`
- `trustScore`
- `trustTier`
- `rejectionReason`
- `createdAt`

### Suspend NGO
`POST /api/admin/ngos/{id}/suspend`

Response:
```json
{
  "message": "NGO suspended."
}
```

### Admin stats
`GET /api/admin/stats`

Frontend should treat this response as a dynamic object and only render keys it needs.
Current keys include:
- `totalUsers`
- `totalDonors`
- `totalNgos`
- `pendingNgos`
- `approvedNgos`
- `suspendedNgos`
- `totalNeeds`
- `activeNeeds`
- `fulfilledNeeds`
- `totalPledges`
- `activePledges`
- `fulfilledPledges`
- `pledgesToday`
- `fulfillmentsThisMonth`
- `totalReports`

### Admin NGO needs inspection
`GET /api/admin/ngos/{id}/needs`

This returns the same safe need-detail shape documented in the needs section and is intended for admin review or inline NGO expansion flows.

### Not currently supported
These workflows are not yet part of the agreed backend contract:
- report dismissal or resolution endpoint
- NGO reinstate endpoint

## 10. Frontend Responsibilities

- Use the OTP-first registration flow exactly as documented.
- Do not use link-based verification for new signup.
- Treat successful registration as an authenticated session and persist the returned JWT immediately.
- Send `Bearer` token on protected routes.
- Handle `204` responses without trying to parse JSON.
- Read validation errors from `fieldErrors` when present.
- Do not depend on undocumented nested entity fields.
- Prefer Swagger for exact field names and sample schemas.

## 11. Backend Responsibilities

- Keep Swagger updated when endpoint contracts change.
- Keep this agreement updated for any frontend-impacting change.
- Avoid breaking field names or response shapes without coordination.
- Return consistent validation and runtime error formats.

## 12. Change Management

Any breaking API change should be treated as coordinated work between frontend and backend.

At minimum:
1. Update backend code.
2. Update Swagger.
3. Update this agreement document.
4. Notify frontend before the change is consumed.
