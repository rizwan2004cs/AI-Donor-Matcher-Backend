package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // 37-byte key => 296-bit HMAC-SHA256 key (minimum 256 bits required)
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("TestSecretKey_AtLeast32Bytes_ForJWT!".getBytes());
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .fullName("Alice Donor")
                .email("alice@example.com")
                .password("encoded-password")
                .role(Role.DONOR)
                .emailVerified(true)
                .build();
    }

    // --- generateToken ---

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(buildUser());
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_tokenHasThreeJwtParts() {
        String token = jwtService.generateToken(buildUser());
        assertThat(token.split("\\.")).hasSize(3);
    }

    // --- extractEmail ---

    @Test
    void extractEmail_returnsCorrectEmail() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
    }

    // --- isValid ---

    @Test
    void isValid_returnsTrueForMatchingEmailAndFreshToken() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isValid(token, user)).isTrue();
    }

    @Test
    void isValid_returnsFalseWhenEmailDoesNotMatch() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        // Create a different UserDetails whose username differs
        User other = User.builder()
                .id(2L)
                .email("other@example.com")
                .role(Role.DONOR)
                .build();
        assertThat(jwtService.isValid(token, other)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        // Use a very small expiration so the token expires immediately
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isValid(token, user)).isFalse();
    }

    // --- invalid token ---

    @Test
    void extractEmail_throwsForMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }
}
