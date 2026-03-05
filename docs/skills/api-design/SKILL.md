---
name: api-design
description: REST API design conventions, error response format, Swagger/OpenAPI configuration, and endpoint documentation standards for the AI Donor Matcher backend. Use when designing new endpoints, setting up Swagger UI with the project theme, or standardizing API responses.
---

# API Design & Documentation

## URL Structure
```
/api/{resource}           → collection
/api/{resource}/{id}      → single item
/api/{resource}/{id}/verb → action on item
```

### Path Conventions
| Pattern | Example | Usage |
|---------|---------|-------|
| Plural nouns | `/api/ngos`, `/api/pledges` | Collection endpoints |
| kebab-case | `/api/auth/resend-verification` | Multi-word paths |
| Actions as sub-paths | `/api/needs/{id}/fulfill` | State transitions |
| My-resource prefix | `/api/ngo/my/profile` | Authenticated user's own data |

## HTTP Methods
| Method | Usage | Response Code |
|--------|-------|---------------|
| GET | Read data | 200 |
| POST | Create resource | 201 |
| PUT | Full update | 200 |
| PATCH | Partial update / state change | 200 |
| DELETE | Remove | 204 (no body) |

## Request/Response Format

### Success — Resource
```json
{
  "id": 1,
  "name": "Hope Foundation",
  "status": "APPROVED"
}
```

### Success — Message only
```json
{
  "message": "Registration successful. Check your email."
}
```

### Error — Business logic
```json
{
  "error": "Email already registered."
}
```

### Error — Validation
```json
{
  "error": "Validation failed.",
  "fieldErrors": {
    "email": "must not be blank",
    "fullName": "must not be blank"
  }
}
```

## Endpoint Reference (27 endpoints)
### Public (no auth)
| # | Method | Endpoint | Feature |
|---|--------|----------|---------|
| 1 | POST | `/api/auth/register` | 1.1 |
| 2 | GET | `/api/auth/verify?token=` | 1.2 |
| 3 | POST | `/api/auth/login` | 1.3 |
| 4 | POST | `/api/auth/resend-verification` | 1.4 |
| 5 | GET | `/api/ngos?lat=&lng=&radius=&category=&search=` | 2.1 |
| 6 | GET | `/api/ngos/{id}` | 2.2 |

### ROLE_NGO
| # | Method | Endpoint | Feature |
|---|--------|----------|---------|
| 8 | GET | `/api/ngo/my/profile` | 3.1 |
| 9 | PUT | `/api/ngo/my/profile` | 3.2 |
| 10 | POST | `/api/ngo/my/photo` | 3.3 |
| 11 | GET | `/api/ngo/my/needs` | 4.1 |
| 12 | POST | `/api/needs` | 4.2 |
| 13 | PUT | `/api/needs/{id}` | 4.3 |
| 14 | DELETE | `/api/needs/{id}` | 4.4 |
| 15 | PATCH | `/api/needs/{id}/fulfill` | 4.5 |

### ROLE_DONOR
| # | Method | Endpoint | Feature |
|---|--------|----------|---------|
| 16 | POST | `/api/pledges` | 5.1 |
| 17 | DELETE | `/api/pledges/{id}` | 5.2 |
| 18 | GET | `/api/pledges/active` | 5.3 |
| 19 | GET | `/api/pledges/history` | 5.4 |

### ROLE_ADMIN
| # | Method | Endpoint | Feature |
|---|--------|----------|---------|
| 20 | GET | `/api/admin/ngos/pending` | 6.1 |
| 21 | POST | `/api/admin/ngos/{id}/approve` | 6.2 |
| 22 | POST | `/api/admin/ngos/{id}/reject` | 6.3 |
| 23 | POST | `/api/admin/ngos/{id}/suspend` | 6.4 |
| 24 | GET | `/api/admin/reports` | 7.1 |
| 25 | PUT | `/api/admin/needs/{id}` | 7.2 |
| 26 | DELETE | `/api/admin/needs/{id}` | 7.3 |
| 27 | GET | `/api/admin/stats` | 7.4 |

### Any authenticated
| # | Method | Endpoint | Feature |
|---|--------|----------|---------|
| 7 | POST | `/api/ngos/{id}/report` | 2.3 |

## Swagger/OpenAPI Setup

### Dependencies (add to pom.xml)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### application.properties
```properties
# Swagger / OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.operations-sorter=method
```

### SecurityConfig — permit Swagger
```java
.requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
```

### OpenAPI Config Bean
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Donor Matcher API")
                .description("REST API for connecting donors with verified NGOs")
                .version("1.0.0")
                .contact(new Contact().name("AI Donor Matcher").email("support@aidonormatcher.com")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### Themed Swagger UI (Earth & Trust)
Add a custom CSS file at `src/main/resources/static/swagger-custom.css` and configure:
```properties
springdoc.swagger-ui.css-url=/swagger-custom.css
```
The theme CSS should use the brand palette:
- Terracotta `#C1694F` for primary actions and topbar
- Forest green `#3B6B4B` for success indicators
- Warm cream `#FAF7F2` background
- `#2C2C2C` text
See the swagger-theme skill or `src/main/resources/static/swagger-custom.css` for full styles.
