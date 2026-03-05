---
name: testing-standards
description: Unit and integration testing conventions for the AI Donor Matcher backend. Use when writing new tests, updating existing tests, or reviewing test quality. Covers Mockito patterns, assertion style, and test structure used across all 11 service test files.
---

# Testing Standards

## Framework Stack
- JUnit 5 (`@Test`, `@ExtendWith`)
- Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`)
- AssertJ (`assertThat`, `assertThatThrownBy`)
- Spring Boot Test (for integration tests only)

## Test File Structure
```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {

    @Mock private SomeRepository someRepository;
    @Mock private OtherService otherService;

    @InjectMocks
    private SomeService someService;

    // Optional shared fixtures
    private User user;
    private Ngo ngo;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com").role(Role.DONOR).build();
        // ... more fixtures
    }

    // ─── methodName ──────────────────────────────────────────────────

    @Test
    void methodName_scenario_expectedResult() {
        // Arrange
        when(someRepository.findById(1L)).thenReturn(Optional.of(entity));

        // Act
        var result = someService.method(1L);

        // Assert
        assertThat(result.getName()).isEqualTo("expected");
        verify(someRepository).save(any(Entity.class));
    }
}
```

## Naming Convention
```
methodName_scenario_expectedResult
```
Examples:
- `register_donorSucceeds_savesUserAndSendsEmail`
- `login_wrongPassword_throwsBadCredentialsException`
- `getMyProfile_userNotFound_throwsRuntimeException`
- `updateProfile_updatesFieldsAndSaves`

## Section Dividers
Group tests by method using comment dividers:
```java
// ─── register ────────────────────────────────────────────────────────────

@Test
void register_donorSucceeds_savesUserAndSendsEmail() { ... }

@Test
void register_duplicateEmail_throwsRuntimeException() { ... }

// ─── login ───────────────────────────────────────────────────────────────

@Test
void login_validCredentials_returnsLoginResponseWithToken() { ... }
```

## Assertion Style — AssertJ Only
```java
// Value assertions
assertThat(result.getEmail()).isEqualTo("alice@example.com");
assertThat(result.isEmailVerified()).isTrue();
assertThat(result.getToken()).isNotNull();

// Exception assertions
assertThatThrownBy(() -> service.method(args))
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("User not found");

// Collection assertions
assertThat(results).hasSize(3);
assertThat(results).extracting("name").contains("Test NGO");
```
Do NOT use JUnit `assertEquals` / `assertThrows` — this project uses AssertJ exclusively.

## Mockito Patterns
```java
// Stubbing
when(repo.findByEmail("test@test.com")).thenReturn(Optional.of(user));
when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

// Verification
verify(repo).save(captor.capture());
verify(emailService).sendVerificationEmail(eq(saved), anyString());
verifyNoInteractions(ngoRepository);

// Argument capture
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repo).save(captor.capture());
User saved = captor.getValue();
assertThat(saved.getEmail()).isEqualTo("test@test.com");
```

## Test Data Patterns
Use `@Builder` for test entities — concise and readable:
```java
User user = User.builder()
    .id(1L)
    .email("alice@example.com")
    .password("encoded")
    .role(Role.DONOR)
    .emailVerified(true)
    .build();
```

## What to Test
For **services**: Every public method, covering:
1. Happy path (success scenario)
2. Failure/edge cases (not found, invalid input, wrong role)
3. Side effects (verify saves, email sends, cascading deletes)

For **controllers** (when created):
- Use `@WebMvcTest(Controller.class)` + `@MockBean` services
- Test HTTP status codes, response shapes, validation errors
- Test auth role restrictions

## What NOT to Do
- Don't use `@SpringBootTest` for unit tests — too slow
- Don't test private methods directly
- Don't mock the class under test
- Don't add tests for getters/setters
- Don't use `@Nested` classes — keep it flat with section dividers
