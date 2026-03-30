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
- Frontend stores the token returned by login
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
6. Backend creates the account only if the OTP is valid.

### Important behavior
- No account is created before OTP-backed registration succeeds.
- Successful registration creates the user as already verified.
- Frontend must not use link-based verification for new registration.
- `GET /api/auth/verify` exists only as a legacy endpoint and is not part of the active frontend flow.

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
  "message": "Registration successful. You can now log in."
}
```

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

## 7. Needs Contract

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

## 10. Frontend Responsibilities

- Use the OTP-first registration flow exactly as documented.
- Do not use link-based verification for new signup.
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
