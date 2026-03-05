# AI Donor Matcher — Spring Boot Backend

## Project
- Spring Boot 3.2.5, Java 21, Maven, PostgreSQL, Spring Security + JWT
- Package: com.aidonormatcher.backend
- Architecture: Controller → Service → Repository (no manager layers)

## Conventions
- DTOs: Java records with Jakarta validation
- Entities: @Entity + Lombok @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
- Services: @Service @RequiredArgsConstructor, constructor-injected final fields
- Repositories: JpaRepository<Entity, Long>, @Repository
- Errors: throw RuntimeException (caught by GlobalExceptionHandler) or BadCredentialsException
- Auth: extract email via Authentication.getName()
- Transactions: @Transactional on multi-write service methods

## Naming
- Classes: PascalCase (NeedService, AuthController)
- Methods/vars: camelCase (findByEmail, trustScore)
- Endpoints: kebab-case (/api/auth/resend-verification)
- DB columns: snake_case (JPA default)

## Do NOT
- Create abstract base classes or generic service interfaces
- Add Swagger annotations unless explicitly asked
- Change DTOs from records to classes
- Add logging unless requested
- Create utility classes for single-use operations

## Status
- Complete: 11 services, all entities, repositories, DTOs, configs
- Missing: 6 controllers + GlobalExceptionHandler (see docs/Feature_implementations/)
- Tests: 69 unit tests, ALL PASSING

## Brand Colors (Earth & Trust)
- Primary: #C1694F (terracotta) | Dark: #9E4A34
- Secondary: #3B6B4B (forest green) | Dark: #2A4D36
- Background: #FAF7F2 | Surface: #FFFFFF
- Text: #2C2C2C | Muted: #6B6B6B | Border: #E8E2D9

## Skills
See docs/skills/ for detailed implementation guides.

## Build
mvn clean compile | mvn test | mvn spring-boot:run

## Session Start (run before touching any code)
```bash
git checkout main
git fetch origin
git status
git pull origin main
```
Returning to a feature branch? Run `git rebase origin/main` after switching back.
Full workflow: see docs/CONTRIBUTING.md

## AI Agent Instruction Files
| File | Agent |
|------|-------|
| `.github/copilot-instructions.md` | GitHub Copilot |
| `.cursorrules` | Cursor |
| `.windsurfrules` | Windsurf |
| `CLAUDE.md` | Claude Code |
| `.clinerules` | Cline |
| `AGENTS.md` | OpenAI Codex |
