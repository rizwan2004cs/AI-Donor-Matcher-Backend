# AI Donor Matcher Backend

Spring Boot REST API for NGO discovery, pledging, moderation, and Firebase-backed authentication.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Java 21, Spring Boot 3.2.5 |
| Security | Spring Security, Firebase ID tokens, legacy JWT compatibility |
| Database | PostgreSQL (Neon / Supabase compatible), Spring Data JPA |
| Auth Emails | Firebase Auth |
| Transactional Email | Resend over HTTPS (optional) |
| Image Hosting | Cloudinary |
| Build | Maven, JaCoCo |
| Deployment | Render |

## Current Auth Model

- Frontend authenticates with Firebase Email/Password.
- Frontend sends `Authorization: Bearer <firebase-id-token>` to the backend.
- Backend resolves local users and roles with:
  - `POST /api/auth/firebase/register`
  - `POST /api/auth/firebase/login`
- Legacy OTP and password-login code remains server-side for compatibility only and is not the primary deployed flow.

## Key Features

- Public NGO discovery via `GET /api/ngos`
- Full NGO detail via `GET /api/ngos/{id}`
- Donor pledge flow via `/api/pledges/**`
- NGO needs management and incoming pledge handling
- Admin NGO approval, suspension, reporting, and moderation
- Firebase legacy-user migration support, disabled by default

## Local Setup

### 1. Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL database
- Firebase project with Email/Password enabled

### 2. Environment

Copy `.env.example` to `.env` and fill in the required values.

Required for local development:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_BASE_URL`
- `FIREBASE_ADMIN_CREDENTIALS_JSON`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Optional for transactional email only:

- `EMAIL_PROVIDER`
- `RESEND_API_KEY`
- `MAIL_FROM`

### 3. Run

```powershell
. .\setup-env.ps1
mvn spring-boot:run
```

The app runs on `http://localhost:8080` by default.

Useful verification URLs:

- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/swagger-ui.html`

## API Summary

### Public

- `POST /api/auth/firebase/register`
- `POST /api/auth/firebase/login`
- `GET /api/ngos`
- `GET /api/ngos/{id}`
- `GET /api/needs/{id}`

### Donor

- `POST /api/pledges`
- `GET /api/pledges/active`
- `GET /api/pledges/history`
- `GET /api/pledges/{id}`
- `DELETE /api/pledges/{id}`

### NGO

- `GET /api/ngo/my/profile`
- `PUT /api/ngo/my/profile`
- `POST /api/ngo/my/photo`
- `GET /api/ngo/my/pledges`
- `PATCH /api/ngo/my/pledges/{pledgeId}/receive`
- `POST /api/needs`
- `PUT /api/needs/{id}`
- `DELETE /api/needs/{id}`
- `PATCH /api/needs/{id}`

### Admin

- `GET /api/admin/stats`
- `GET /api/admin/ngos/pending`
- `POST /api/admin/ngos/{id}/approve`
- `POST /api/admin/ngos/{id}/reject`
- `POST /api/admin/ngos/{id}/suspend`
- `GET /api/admin/reports`

## Render Deployment

Use the included `render.yaml` blueprint.

Production envs required in Render:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_BASE_URL`
- `CORS_ALLOWED_ORIGIN_PATTERNS`
- `FIREBASE_ADMIN_CREDENTIALS_JSON`
- `FIREBASE_MIGRATION_ENABLED=false`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Optional transactional mail envs:

- `EMAIL_PROVIDER`
- `RESEND_API_KEY`
- `MAIL_FROM`

Deployment notes:

- Auth emails are handled by Firebase, not the backend mail system.
- If mail envs are omitted, transactional email methods safely no-op.
- Health check path is `/v3/api-docs`.
- Render injects `PORT`; Spring reads it from `server.port=${PORT:8080}`.

## Documentation

- [`docs/DEPLOYMENT_RENDER.md`](docs/DEPLOYMENT_RENDER.md)
- [`docs/BACKEND.md`](docs/BACKEND.md)
- [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md)
