---
name: controller-implementation
description: Guide for implementing REST controllers in the AI Donor Matcher backend. Use when building any of the 6 missing controllers (GlobalExceptionHandler, AuthController, NgoController, NeedController, PledgeController, AdminController). Contains endpoint specs, response patterns, and the implementation order.
---

# Controller Implementation Guide

## Prerequisites
The GlobalExceptionHandler MUST be created before any controller. See Step 0 below.

## Implementation Order
Build controllers in this exact sequence — each builds on the previous:

### Step 0: GlobalExceptionHandler
Location: `controller/GlobalExceptionHandler.java`
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Handle RuntimeException → 400 BAD_REQUEST with { "error": message }
    // Handle BadCredentialsException → 401 UNAUTHORIZED
    // Handle AccessDeniedException → 403 FORBIDDEN
    // Handle MethodArgumentNotValidException → 400 with field errors map
}
```
Response format — always return:
```json
{ "error": "Human-readable message." }
```
For validation errors:
```json
{ "error": "Validation failed.", "fieldErrors": { "email": "must not be blank" } }
```

### Step 1: AuthController
Path: `/api/auth`  |  Auth: None (public)
| Method | Endpoint | Service Call | Response |
|--------|----------|-------------|----------|
| POST | `/register` | `authService.register(req)` | 201 `{ "message": "..." }` |
| GET | `/verify?token=` | `authService.verifyEmail(token)` | 200 `{ "message": "..." }` |
| POST | `/login` | `authService.login(req)` | 200 `LoginResponse` |
| POST | `/resend-verification` | (Feature 1.4 — may not exist yet) | 200 |

### Step 2: NgoController (Profile)
Path: `/api/ngo/my`  |  Auth: ROLE_NGO
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| GET | `/profile` | `ngoService.getMyProfile(email)` |
| PUT | `/profile` | `ngoService.updateProfile(email, req)` |
| POST | `/photo` | `cloudinaryService.upload(file)` → `ngoService.updatePhotoUrl(email, url)` |
| GET | `/needs` | `needService.getMyNeeds(email)` |

### Step 3: NeedController
Path: `/api/needs`  |  Auth: ROLE_NGO
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `needService.createNeed(email, req)` |
| PUT | `/{id}` | `needService.updateNeed(email, id, req)` |
| DELETE | `/{id}` | `needService.deleteNeed(email, id)` |
| PATCH | `/{id}/fulfill` | `needService.markFulfilled(email, id)` |

**SecurityConfig change needed:** Add `.requestMatchers(HttpMethod.POST, "/api/needs/**").hasRole("NGO")` etc.

### Step 4: NgoController (Discovery — public)
Path: `/api/ngos`  |  Auth: None
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| GET | `/?lat=&lng=&radius=&category=&search=` | `ngoService.discoverNgos(...)` |
| GET | `/{id}` | `ngoService.getNgoById(id)` |

### Step 5: PledgeController
Path: `/api/pledges`  |  Auth: ROLE_DONOR
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `pledgeService.createPledge(email, req)` |
| DELETE | `/{id}` | `pledgeService.cancelPledge(email, id)` |
| GET | `/active` | `pledgeService.getActive(email)` |
| GET | `/history` | `pledgeService.getHistory(email)` |

### Steps 6+7: AdminController
Path: `/api/admin`  |  Auth: ROLE_ADMIN
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| GET | `/ngos/pending` | `adminService.getPendingNgos()` |
| POST | `/ngos/{id}/approve` | `adminService.approveNgo(id)` |
| POST | `/ngos/{id}/reject` | `adminService.rejectNgo(id, reason)` |
| POST | `/ngos/{id}/suspend` | `adminService.suspendNgo(id, reason)` |
| GET | `/reports` | `reportService.getAllReports()` |
| PUT | `/needs/{id}` | `adminService.editNeed(id, req)` |
| DELETE | `/needs/{id}` | `adminService.deleteNeed(id)` |

### Step 8: Report Endpoint (add to NgoController)
| Method | Endpoint | Auth | Service Call |
|--------|----------|------|-------------|
| POST | `/api/ngos/{id}/report` | Any authenticated | `reportService.createReport(email, id, req)` |

## Controller Code Pattern
```java
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req,
                                     Authentication auth) {
        String email = auth.getName();
        var result = resourceService.create(email, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }
}
```

## Response Status Codes
| Action | Status | When |
|--------|--------|------|
| Create resource | 201 CREATED | POST success |
| Read/Update | 200 OK | GET/PUT/PATCH success |
| Delete | 204 NO_CONTENT | DELETE success |
| Message-only | 200 OK | `{ "message": "..." }` |
| Validation fail | 400 BAD_REQUEST | `@Valid` errors |
| Auth fail | 401 UNAUTHORIZED | Bad credentials |
| Forbidden | 403 FORBIDDEN | Wrong role |
| Not found | 400 BAD_REQUEST | Business `RuntimeException` |
