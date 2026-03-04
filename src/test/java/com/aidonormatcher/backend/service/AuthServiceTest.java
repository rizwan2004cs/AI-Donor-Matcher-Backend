package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private NgoRepository ngoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_donorSucceeds_savesUserAndSendsEmail() {
        RegisterRequest req = new RegisterRequest(
                "Alice", "alice@example.com", "password123", Role.DONOR, "London");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(Role.DONOR);
        assertThat(saved.getEmailVerificationToken()).isNotNull();
        verify(emailService).sendVerificationEmail(eq(saved), anyString());
        verifyNoInteractions(ngoRepository);
    }

    @Test
    void register_ngoRoleAlsoCreatesNgoEntity() {
        RegisterRequest req = new RegisterRequest(
                "NGO Org", "ngo@org.com", "secret", Role.NGO, null);
        when(userRepository.existsByEmail("ngo@org.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(ngoRepository).save(any(Ngo.class));
        ArgumentCaptor<Ngo> ngoCaptor = ArgumentCaptor.forClass(Ngo.class);
        verify(ngoRepository).save(ngoCaptor.capture());
        assertThat(ngoCaptor.getValue().getStatus()).isEqualTo(NgoStatus.PENDING);
    }

    @Test
    void register_duplicateEmail_throwsRuntimeException() {
        RegisterRequest req = new RegisterRequest(
                "Alice", "alice@example.com", "pw", Role.DONOR, null);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }

    // ─── verifyEmail ─────────────────────────────────────────────────────────

    @Test
    void verifyEmail_validToken_setsEmailVerifiedAndClearsToken() {
        User user = User.builder()
                .id(1L).email("test@test.com")
                .emailVerificationToken("abc-token")
                .emailVerified(false)
                .build();
        when(userRepository.findByEmailVerificationToken("abc-token"))
                .thenReturn(Optional.of(user));

        authService.verifyEmail("abc-token");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationToken()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_invalidToken_throwsRuntimeException() {
        when(userRepository.findByEmailVerificationToken("bad-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid verification token");
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsLoginResponseWithToken() {
        User user = User.builder()
                .id(1L).email("alice@example.com").password("encoded")
                .fullName("Alice").role(Role.DONOR).emailVerified(true)
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token-abc");

        LoginResponse resp = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertThat(resp.token()).isEqualTo("jwt-token-abc");
        assertThat(resp.email()).isEqualTo("alice@example.com");
        assertThat(resp.role()).isEqualTo("DONOR");
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        User user = User.builder()
                .id(1L).email("alice@example.com").password("encoded")
                .role(Role.DONOR).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsBadCredentialsException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
