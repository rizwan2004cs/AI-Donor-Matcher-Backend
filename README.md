# AI Donor Matcher — Backend

> A Spring Boot REST API that connects verified NGOs with nearby donors through location-aware discovery, real-time resource needs, and a trust-scored pledge system.

---

## Table of Contents

- [Problem & Solution](#problem--solution)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [REST API Reference](#rest-api-reference)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Building & Running](#building--running)
- [Deployment (Render)](#deployment-render)
- [Contributing](#contributing)
- [Documentation](#documentation)

---

## Problem & Solution

**Problem:** Donors can't verify nearby NGOs or know what they actually need. NGOs receive wrong items. No unified platform exists to bridge the gap.

**Solution:** A location-aware platform where:

- **Admins** verify NGOs to maintain a trust filter
- **NGOs** post specific, real-time resource needs
- **Donors** discover nearby verified NGOs via a map (Haversine geospatial queries), pledge items, and get navigation assistance
- **Trust scores** track NGO credibility and are recalculated daily
- **Email notifications** keep all parties informed at every step

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Java 21 · Spring Boot 3.2.5 |
| Security | Spring Security · JWT (JJWT 0.12.3) |
| Database | PostgreSQL (Supabase / Neon free tier) · Spring Data JPA |
| Email | Spring Mail (Gmail SMTP) |
| Image Hosting | Cloudinary |
| Build | Maven · JaCoCo (code coverage) |
| Deployment | Render (free tier) |

---

## Project Structure

```
src/main/java/com/aidonormatcher/backend/
├── config/
│   ├── SecurityConfig.java         # Spring Security + CORS + endpoint rules
│   ├── JwtFilter.java              # JWT extraction + validation per request
│   ├── CloudinaryConfig.java       # Cloudinary bean
│   └── ApplicationConfig.java      # PasswordEncoder, AuthenticationManager beans
├── controller/
│   ├── GlobalExceptionHandler.java # Maps exceptions to HTTP status codes
│   ├── AuthController.java         # Register, verify email, login
│   ├── NgoController.java          # NGO profile + public discovery + reports
│   ├── NeedController.java         # Needs CRUD + fulfillment
│   ├── PledgeController.java       # Pledge creation, cancellation, history
│   └── AdminController.java        # NGO verification + content moderation
├── service/
│   ├── AuthService.java            # Registration, email verification, login
│   ├── NgoService.java             # Profile management, discovery, verification
│   ├── NeedService.java            # Needs CRUD + auto-fulfillment
│   ├── PledgeService.java          # Pledge logic (split-pledging, cancellation)
│   ├── AdminService.java           # NGO approval/rejection, moderation
│   ├── ReportService.java          # Donor report submission
│   ├── TrustScoreService.java      # Trust tier calculation (daily)
│   ├── ScheduledJobService.java    # Background jobs (pledge expiry, need expiry)
│   ├── EmailService.java           # 12 email templates via JavaMail
│   ├── JwtService.java             # Token generation and validation
│   └── CloudinaryService.java      # Image uploads
├── repository/
│   ├── UserRepository.java
│   ├── NgoRepository.java
│   ├── NeedRepository.java
│   ├── PledgeRepository.java
│   └── ReportRepository.java
├── entity/
│   ├── User.java
│   ├── Ngo.java
│   ├── Need.java
│   ├── Pledge.java
│   └── Report.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── NgoProfileRequest.java
│   ├── NgoDiscoveryDTO.java
│   ├── NeedRequest.java
│   ├── PledgeRequest.java
│   ├── PledgeResponse.java
│   └── ReportRequest.java
└── enums/
    ├── Role.java                   # DONOR, NGO, ADMIN
    ├── NgoStatus.java              # PENDING, APPROVED, REJECTED, SUSPENDED
    ├── NeedStatus.java             # ACTIVE, FULFILLED, EXPIRED, CLOSED
    ├── NeedCategory.java           # Clothing, Food, Medical, Education, etc.
    ├── UrgencyLevel.java           # CRITICAL, HIGH, MEDIUM, LOW
    ├── PledgeStatus.java           # ACTIVE, FULFILLED, CANCELLED, EXPIRED
    └── TrustTier.java              # EMERGING, DEVELOPING, TRUSTED, EXEMPLARY
```

---

## Features

### Authentication
- User registration (Donor or NGO) with optional document upload
- Email verification via token link
- JWT-based login with role-encoded tokens

### NGO Discovery (Public)
- Location-based NGO search using Haversine distance formula
- Filter by radius, category, and keyword search
- View full NGO public profile with active needs

### NGO Profile Management
- Get, update, and complete NGO profile (name, address, description, phone, category)
- Upload profile photo via Cloudinary
- Go-live gate — NGOs must complete all required fields before appearing on the map

### Needs Management
- Create up to 5 active resource needs per NGO
- Edit or delete needs (blocked if any pledge exists)
- Mark needs as fulfilled (triggers trust score recalculation)

### Pledge System
- Donors pledge specific quantities against a need (split-pledging supported)
- Pessimistic locking prevents over-pledging
- Cancel active pledges (quantity restored to need)
- View active pledges and full donation history

### Admin — NGO Verification
- Review pending NGO applications
- Approve, reject (with reason), or suspend NGOs
- Suspension cascade: closes all needs, cancels all pledges, notifies affected donors

### Admin — Content Moderation
- View donor-submitted reports
- Edit or remove any NGO's need (cascade: cancel pledges, notify donors)

### Background Jobs
- Auto-expire pledges older than 48 hours (runs hourly)
- Close expired needs (runs daily at midnight)
- Warn NGOs of needs expiring within 3 days
- Daily trust score recalculation with recency penalty

### Email Notifications (12 templates)
- Email verification on registration
- Admin notification on new NGO application
- NGO approval / rejection emails
- Pledge confirmation to donor (includes NGO address + contact)
- Pledge notification to NGO
- Pledge cancelled / auto-expired notifications
- Need removal notification to affected donors
- NGO suspension notification to affected donors
- Need fulfillment thank-you email to donor
- Need expiry warning to NGO (3 days before)

---

## REST API Reference

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a donor or NGO |
| `GET` | `/api/auth/verify?token=` | Verify email via token |
| `POST` | `/api/auth/login` | Login and receive JWT |
| `GET` | `/api/ngos` | Discover nearby NGOs (supports `lat`, `lng`, `radius`, `category`, `search` params) |
| `GET` | `/api/ngos/{id}` | Get full public NGO profile |

### Authenticated — Any Role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/ngos/{id}/report` | Submit a report against an NGO |

### Authenticated — NGO Role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ngo/my/profile` | Get own NGO profile |
| `PUT` | `/api/ngo/my/profile` | Update own NGO profile |
| `POST` | `/api/ngo/my/photo` | Upload profile photo |
| `GET` | `/api/ngo/my/needs` | List own needs |
| `POST` | `/api/needs` | Create a new resource need |
| `PUT` | `/api/needs/{id}` | Edit a need |
| `DELETE` | `/api/needs/{id}` | Delete a need |
| `PATCH` | `/api/needs/{id}/fulfill` | Mark a need as fulfilled |

### Authenticated — Donor Role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/pledges` | Create a pledge against a need |
| `DELETE` | `/api/pledges/{id}` | Cancel an active pledge |
| `GET` | `/api/pledges/active` | Get all active pledges |
| `GET` | `/api/pledges/history` | Get full pledge donation history |

### Authenticated — Admin Role

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/ngos/pending` | List all pending NGO applications |
| `POST` | `/api/admin/ngos/{id}/approve` | Approve an NGO |
| `POST` | `/api/admin/ngos/{id}/reject` | Reject an NGO with reason |
| `POST` | `/api/admin/ngos/{id}/suspend` | Suspend an NGO (cascade) |
| `GET` | `/api/admin/reports` | View all donor-submitted reports |
| `PUT` | `/api/admin/needs/{id}` | Edit any NGO's need |
| `DELETE` | `/api/admin/needs/{id}` | Remove a need (cascade) |
| `GET` | `/api/admin/stats` | Platform stats overview |

---

## Getting Started (First-Time Setup)

Follow these complete steps if you are running this repository for the first time.

### 1. Prerequisites

Ensure you have the following installed on your machine:
- **Java 21** (JDK)
- **Maven 3.9+**
- **Git 2.x**
- **PostgreSQL** database (Local or cloud-based like Supabase/Neon)

### 2. Clone the Repository

```bash
git clone https://github.com/rizwan2004cs/AI-Donor-Matcher-Backend.git
cd AI-Donor-Matcher-Backend
```

### 3. Environment Setup (`application.properties`)

Create or update the `src/main/resources/application.properties` file for local development. **Never hardcode secrets** in source control. You will need to replace the placeholders with your actual credentials.

```properties
# Database (PostgreSQL — use your local DB or a cloud string)
spring.datasource.url=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT (Generate a secure base64 string for local testing)
jwt.secret=your-256-bit-base64-encoded-secret-here
jwt.expiration-ms=86400000

# Email (Gmail SMTP — Use an App Password, not your standard password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-gmail-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Cloudinary (Sign up for a free tier for image uploads)
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# App Configuration
app.base-url=http://localhost:5173 # Or your frontend's running port (e.g., React/Vite)
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

### 4. Build & Run

Once your `application.properties` is configured, build and start the server:

```bash
# Clean and compile the project (downloads maven dependencies)
mvn clean compile

# Run the application locally
mvn spring-boot:run
```

By default, the server will start on `http://localhost:8080`.

**Testing the Application:**
Test endpoints manually with Postman, curl, or the VS Code REST Client extension. Test the `/api/ngos` public discovery endpoint to ensure proper database connectivity.

---

## Deployment (Render)

1. Push the repo to GitHub.
2. Go to [render.com](https://render.com) → **New Web Service** → Connect the GitHub repo.
3. Configure the service:
   - **Environment:** Java
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/backend-0.0.1-SNAPSHOT.jar`
4. Add all environment variables from the section above in Render's **Environment** tab.
5. The free tier spins down after 15 minutes of inactivity. The first request after idle takes ~30 seconds — this is expected for a prototype.

---

## Contributing

See the full [Contributing Guide](docs/CONTRIBUTING.md) for detailed instructions. Key points:

- **Branch naming:** `feature/<area>/<short-description>` or `fix/<area>/<short-description>`
- **Commit format:** `<type>(<area>): <short summary>` (e.g., `feat(auth): add register and login endpoints`)
- **Controllers should be thin** — delegate all business logic to the service layer
- **Security:** Never bypass `SecurityConfig`; always read user identity from `@AuthenticationPrincipal`
- **Database:** Keep `ddl-auto=update` — never use `create` or `create-drop`

### Recommended Build Order for Controllers

Controllers should be built in this sequence to enable end-to-end testing at each stage:

1. **Authentication** — register, verify, login (unblocks all other features)
2. **NGO Profile Management** — required before NGOs can go live
3. **Needs Management** — required before pledges work
4. **NGO Public Discovery** — donor-facing map feed
5. **Pledge System** — core donation loop
6. **Admin Verification + Moderation** — NGO approval and content moderation
7. **Report System** — donor report submission

---

## Documentation

Detailed documentation is available in the [`docs/`](docs/) folder:

| Document | Description |
|----------|-------------|
| [BACKEND.md](docs/BACKEND.md) | Full backend architecture guide — setup, entities, security, and all feature logic |
| [FEATURES.md](docs/FEATURES.md) | Feature checklist with implementation status for every endpoint |
| [CONTRIBUTING.md](docs/CONTRIBUTING.md) | Development workflow, branch naming, code standards, and security rules |
| [IMPLEMENTATION_ORDER.md](docs/Feature_implementations/IMPLEMENTATION_ORDER.md) | Step-by-step controller build sequence and service method signatures |
| [Feature Guides](docs/Feature_implementations/) | Individual implementation guides for each feature area |
| [Product Design](docs/AI_Donation_Matcher_FINAL_v4_.md) | Complete product design document with user journeys and state machines |
