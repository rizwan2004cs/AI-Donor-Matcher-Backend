# Render Deployment

## What This Repo Includes

- `render.yaml` blueprint for a Render web service
- `Dockerfile` for Java 21 / Spring Boot packaging
- `docker-entrypoint.sh` that converts a `postgresql://...` Neon connection string into a JDBC URL automatically

The Blueprint uses:

- a free Render web service
- Neon for PostgreSQL
- Resend over HTTPS instead of SMTP, so the free Render plan can still send OTP and notification emails

## Backend Environment Variables

Set these manually in Render before going live:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_BASE_URL`
- `EMAIL_PROVIDER`
- `RESEND_API_KEY`
- `MAIL_FROM`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `GEOCODING_PROVIDER`
- `GEOCODING_NOMINATIM_USER_AGENT`

## CORS

The backend now reads `CORS_ALLOWED_ORIGIN_PATTERNS`.

Default value:

`http://localhost:5173,https://*.vercel.app`

That allows local development plus any preview or production Vercel deployment using a `*.vercel.app` URL. If you later add a custom frontend domain, append it to the env var.

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

## Deployment Notes

- The backend listens on `PORT`, which Render injects automatically.
- Database schema updates still rely on `spring.jpa.hibernate.ddl-auto=update`.
- Email delivery now uses Resend over HTTPS, so the free Render plan remains viable.
- The email retry queue is still in-memory; if the service restarts before retrying, queued emails are lost.
