# Contributing Guide — AI Donor Matcher Backend

This guide explains how to pick up a feature from [`FEATURES.md`](./FEATURES.md), build it on its own branch, and get it merged cleanly.

---

## Prerequisites

- Java 21 (JDK installed at `C:\Program Files\Java\jdk-21`)
- Maven 3.9+ (or use the wrapper if added)
- Git 2.x
- A PostgreSQL database connection (Supabase or Neon — see `application.properties`)
- The repo cloned locally and the `main` branch building successfully

---

## Branch Naming Convention

Every feature lives on its own branch. Use this format:

```
feature/<area>/<short-description>
```

| Example branch | What it covers |
|----------------|---------------|
| `feature/auth/register-login` | Auth controller — register + login endpoints |
| `feature/ngo/profile-management` | NGO profile get/update/photo endpoints |
| `feature/needs/crud-and-fulfill` | Needs controller — create, edit, delete, fulfill |
| `feature/pledges/create-and-cancel` | Pledge creation and cancellation |
| `feature/admin/ngo-verification` | Admin NGO approve/reject/suspend |
| `feature/admin/moderation` | Admin need moderation + report queue |
| `fix/<area>/<short-description>` | Bug fix (e.g. `fix/pledges/expiry-timezone`) |

---

## Step-by-Step Workflow

### 1. Sync with `main` before starting

```bash
git checkout main
git pull origin main
```

### 2. Create your feature branch

```bash
git checkout -b feature/<area>/<short-description>
```

Example:
```bash
git checkout -b feature/auth/register-login
```

### 3. Implement the feature

Each feature in `FEATURES.md` maps to a specific controller. Follow this structure:

```
src/main/java/com/aidonormatcher/backend/
└── controller/
    └── <Name>Controller.java     ← create this file
```

**Controller checklist:**
- [ ] Annotate with `@RestController` and `@RequestMapping`
- [ ] Inject the corresponding service via `@RequiredArgsConstructor`
- [ ] Extract the authenticated user from `@AuthenticationPrincipal UserDetails`
- [ ] Return appropriate HTTP status codes (`201 Created`, `200 OK`, `400`, `403`, `404`)
- [ ] Validate request bodies with `@Valid` where the DTO uses Jakarta validation constraints
- [ ] Do not add business logic to the controller — delegate everything to the service layer

**Example controller skeleton:**
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration successful. Check your email to verify.");
    }
}
```

### 4. Build and verify locally

```bash
# Windows — set JDK 21
set JAVA_HOME=C:\Program Files\Java\jdk-21

# Compile everything (main + test code)
mvn clean test-compile -q

# Run the application locally to smoke-test endpoints
mvn spring-boot:run
```

Test your endpoints manually with Postman, curl, or the VS Code REST Client extension before committing.

### 5. Commit your changes

Write a clear, scoped commit message:

```bash
git add src/main/java/com/aidonormatcher/backend/controller/<Name>Controller.java
git commit -m "feat(auth): add register, verify-email, and login endpoints

- POST /api/auth/register — creates donor or NGO account
- GET  /api/auth/verify   — activates account via token
- POST /api/auth/login    — returns JWT on success
- Delegates all logic to AuthService (no business logic in controller)"
```

**Commit message format:**
```
<type>(<area>): <short summary>

<optional body — what was added and why>
```

| Type | When to use |
|------|-------------|
| `feat` | New endpoint or feature |
| `fix` | Bug fix |
| `refactor` | Code restructure, no behaviour change |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `chore` | Build config, dependency bump |

### 6. Push your branch

```bash
git push -u origin feature/<area>/<short-description>
```

### 7. Open a Pull Request

- **Base branch:** `main`
- **Title:** match your commit message summary
- **Description:** list the endpoints added, any design decisions, and how to test
- Assign at least one team member as a reviewer

### 8. After review — merge and clean up

```bash
# After PR is approved and merged on GitHub:
git checkout main
git pull origin main
git branch -d feature/<area>/<short-description>
```

---

## Code Standards

### Security rules (do not bypass)
- Every non-public endpoint must be secured via `SecurityConfig` — never use `.permitAll()` on authenticated endpoints
- Role checks must be enforced in `SecurityConfig` (HTTP security) **and** validated inside the service method
- Never trust the request body for the authenticated user's ID — always read it from `UserDetails` / the database

### Style
- One controller per feature area (Auth, Ngo, Need, Pledge, Admin, Report)
- Keep controllers thin — one method per endpoint, no logic beyond parsing the request and formatting the response
- Use `@Slf4j` for logging errors; do not use `System.out.println`
- All new DTOs go in `src/main/java/.../dto/`

### Database
- Never use `ddl-auto=create` or `ddl-auto=create-drop` — keep it as `update`
- Add new entity fields via the entity class only — Hibernate will alter the table automatically

---

## Getting Help

- Check [`FEATURES.md`](./FEATURES.md) for the full feature list and build order
- Check [`BACKEND.md`](./BACKEND.md) for detailed service-layer logic descriptions and endpoint specs
- Check [`AI_Donation_Matcher_FINAL_v4_.md`](./AI_Donation_Matcher_FINAL_v4_.md) for the complete product design and user journeys
