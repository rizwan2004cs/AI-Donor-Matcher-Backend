# AI Donor Matcher Backend

Last updated: 2026-03-31

This is the backend-focused reference for the current Spring Boot codebase. It matches the live controller/service/repository implementation and the Firebase-first authentication flow.

See the broader system document for end-to-end diagrams and cross-stack flows:

- [`AI_Donation_Matcher_FINAL_v4_.md`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/docs/AI_Donation_Matcher_FINAL_v4_.md)

---

## 1. Stack

- Java 21
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- PostgreSQL
- Firebase Admin SDK
- Cloudinary
- OpenAPI / Swagger UI

---

## 2. Architecture

### Backend layers

- `controller`: HTTP endpoints
- `service`: business logic and transactions
- `repository`: JPA and native SQL
- `config`: auth, CORS, Firebase, Cloudinary
- `dto`: request and response contracts
- `entity`: persistence model

### Primary flows handled by the backend

- Firebase ID token verification
- local user creation and role resolution
- NGO approval and moderation
- geocoded NGO discovery
- need lifecycle management
- donor pledge lifecycle
- scheduled expiry jobs

---

## 3. Security Model

### Public endpoints

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/api/auth/**`
- `GET /api/ngos/**`
- `GET /api/needs/*`

### Protected by role

- `/api/admin/**` -> `ROLE_ADMIN`
- `/api/ngo/**` -> `ROLE_NGO`
- `/api/pledges/**` -> `ROLE_DONOR`

### Bearer token behavior

- The active frontend sends the Firebase ID token as `Authorization: Bearer <token>`.
- `JwtFilter` verifies the token through the Firebase-backed auth chain and resolves the user identity and authorities.
- Legacy backend login/JWT endpoints still exist for compatibility, but the primary deployed frontend flow is Firebase-first.

---

## 4. Data Model Summary

### `users`

- id
- full_name
- email
- firebase_uid
- password
- role
- email_verified
- legacy verification token / OTP fields
- location
- created_at

### `ngos`

- id
- user_id
- name
- address
- contact_email
- contact_phone
- description
- category_of_work
- photo_url
- document_url
- status
- profile_complete
- lat
- lng
- trust_score
- trust_tier
- verified_at
- last_activity_at
- rejection_reason
- created_at

### `needs`

- id
- ngo_id
- category
- item_name
- description
- quantity_required
- quantity_pledged
- quantity_received
- urgency
- expiry_date
- status
- created_at
- fulfilled_at

### `pledges`

- id
- donor_id
- need_id
- quantity
- status
- created_at
- expires_at

### `reports`

- id
- reporter_id
- ngo_id
- reason
- reported_at

---

## 5. Current Auth Model

### Active path

- Frontend authenticates with Firebase Email/Password.
- Frontend posts Firebase ID token to:
  - `POST /api/auth/firebase/register`
  - `POST /api/auth/firebase/login`
- Backend verifies the token with Firebase Admin.
- Backend creates or links the local `User`.
- Backend returns local role-aware identity metadata.

### Legacy paths still present

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/resend-verification`
- `POST /api/auth/send-registration-otp`
- `POST /api/auth/send-otp`
- `POST /api/auth/verify-otp`
- `GET /api/auth/verify`

These are not the primary frontend flow.

---

## 6. Current API Summary

### Authentication

- `POST /api/auth/firebase/register`
- `POST /api/auth/firebase/login`
- legacy endpoints remain under `/api/auth/**`

### NGO public

- `GET /api/ngos`
- `GET /api/ngos/{id}`
- `POST /api/ngos/{id}/report`
- `GET /api/needs/{id}`

### NGO private

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

### Donor

- `POST /api/pledges`
- `GET /api/pledges/{id}`
- `DELETE /api/pledges/{id}`
- `GET /api/pledges/active`
- `GET /api/pledges/history`

### Admin

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

---

## 7. Current Behavior Notes

### Discovery

- only `APPROVED` NGOs are included
- nearby discovery requires `lat/lng`
- approved NGOs are no longer excluded by `profile_complete=false`

### Pledges

- donor-only
- no `emailVerified` pledge gate anymore
- need row is locked pessimistically during pledge creation
- pledge expiry is 48 hours

### Needs

- max 5 active needs per NGO
- edit/delete blocked after any quantity is pledged
- need becomes fully complete based on `quantity_received`

### Moderation

- donor reports can flag NGOs
- admin suspension cascades through active needs and pledges transactionally

---

## 8. Scheduled Jobs

### Hourly

- expire old active pledges after 48 hours

### Daily

- warn NGOs about needs expiring soon
- expire overdue needs

---

## 9. Environment Variables

### Required

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_BASE_URL`
- `FIREBASE_ADMIN_CREDENTIALS_JSON`
- `FIREBASE_ADMIN_PROJECT_ID`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

### Common defaults

- `JWT_EXPIRATION_MS=86400000`
- `FIREBASE_MIGRATION_ENABLED=false`
- `GEOCODING_PROVIDER=nominatim`
- `GEOCODING_NOMINATIM_USER_AGENT=AI-Donor-Matcher/1.0`

### Optional

- `EMAIL_PROVIDER`
- `RESEND_API_KEY`
- `MAIL_FROM`

Auth mail is handled by Firebase. Backend mail is optional and used only for non-auth transactional notifications.

---

## 10. Local Run

```powershell
. .\setup-env.ps1
mvn spring-boot:run
```

Useful local URLs:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

---

## 11. Deployment

The backend is intended to deploy to Render from:

- [`render.yaml`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/render.yaml)

See:

- [`DEPLOYMENT_RENDER.md`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/docs/DEPLOYMENT_RENDER.md)
- [`FRONTEND_BACKEND_AGREEMENT.md`](C:/Users/moham/FYP/AI-Donor-Matcher-Backend/docs/FRONTEND_BACKEND_AGREEMENT.md)
