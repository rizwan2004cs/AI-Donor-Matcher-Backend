# Render Deployment

## What This Repo Includes

- `render.yaml` blueprint for a Render web service
- `Dockerfile` for Java 21 / Spring Boot packaging
- `docker-entrypoint.sh` that converts a `postgresql://...` Neon connection string into a JDBC URL automatically

The Blueprint uses:

- a free Render web service
- Neon for PostgreSQL
- Firebase Auth for signup, login, email verification, and password reset
- optional Resend over HTTPS for non-auth transactional emails only

## Backend Environment Variables

Set these manually in Render before going live:

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
- `GEOCODING_PROVIDER`
- `GEOCODING_NOMINATIM_USER_AGENT`

Optional only if you want transactional emails at launch:

- `EMAIL_PROVIDER`
- `RESEND_API_KEY`
- `MAIL_FROM`

## CORS

The backend now reads `CORS_ALLOWED_ORIGIN_PATTERNS`.

Default value:

`http://localhost:5173,https://*.vercel.app`

That allows local development plus any preview or production Vercel deployment using a `*.vercel.app` URL. If you later add a custom frontend domain, append it to the env var.

Set `APP_BASE_URL` to the deployed frontend URL, for example:

`https://your-frontend.vercel.app`

## Swagger Verification

After deployment, verify:

- `/swagger-ui.html`
- `/v3/api-docs`

Example:

- `https://your-render-service.onrender.com/swagger-ui.html`
- `https://your-render-service.onrender.com/v3/api-docs`

## Frontend API Base URL

For the Vite frontend, set:

`VITE_API_BASE_URL=https://your-render-service.onrender.com`

Also set all Firebase web app variables in Vercel:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_APP_ID`
- `VITE_OSRM_URL`

## Deployment Notes

- The backend listens on `PORT`, which Render injects automatically.
- Database schema updates still rely on `spring.jpa.hibernate.ddl-auto=update`.
- Auth emails are handled by Firebase, not the backend mail system.
- If `EMAIL_PROVIDER`, `RESEND_API_KEY`, or `MAIL_FROM` are omitted, transactional email methods safely no-op.
- The email retry queue is still in-memory; if the service restarts before retrying, queued emails are lost.
- Keep `FIREBASE_MIGRATION_ENABLED=false` in production so the one-time legacy-user import does not run again.
