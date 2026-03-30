# Render Deployment

## What This Repo Includes

- `render.yaml` blueprint for a Render web service plus PostgreSQL
- `Dockerfile` for Java 21 / Spring Boot packaging
- `docker-entrypoint.sh` that converts Render's `postgresql://...` connection string into a JDBC URL automatically

## Backend Environment Variables

Render will provision these through the blueprint:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `GEOCODING_PROVIDER`
- `GEOCODING_NOMINATIM_USER_AGENT`

Set these manually in Render before going live:

- `APP_BASE_URL`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

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
- Gmail SMTP may hit quota limits; approval/rejection emails are already best-effort with retry.
