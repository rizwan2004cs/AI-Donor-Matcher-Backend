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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private NgoRepository ngoRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private JwtService jwtService;
        @Mock
        private EmailService emailService;
        @Mock
        private RegistrationOtpService registrationOtpService;
        @Mock
        private CloudinaryService cloudinaryService;

        @InjectMocks
        private AuthService authService;

        // ─── register ────────────────────────────────────────────────────────────

        @Test
        void register_donorSucceeds_onlyAfterOtpVerification() {
                RegisterRequest req = new RegisterRequest(
                                "Alice", "alice@example.com", "password123", Role.DONOR, "London", "123456");
                User savedUser = User.builder()
                                .id(1L)
                                .fullName("Alice")
                                .email("alice@example.com")
                                .password("encoded")
                                .role(Role.DONOR)
                                .emailVerified(true)
                                .location("London")
                                .createdAt(LocalDateTime.now())
                                .build();
                when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
                when(passwordEncoder.encode("password123")).thenReturn("encoded");
                when(userRepository.save(any(User.class))).thenReturn(savedUser);
                when(jwtService.generateToken(any(User.class))).thenReturn("jwt-register-token");

                LoginResponse response = authService.register(req, null);

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());
                User saved = captor.getValue();
                assertThat(saved.getEmail()).isEqualTo("alice@example.com");
                assertThat(saved.getPassword()).isEqualTo("encoded");
                assertThat(saved.getRole()).isEqualTo(Role.DONOR);
                assertThat(saved.isEmailVerified()).isTrue();
                assertThat(saved.getCreatedAt()).isNotNull();
                assertThat(response.token()).isEqualTo("jwt-register-token");
                assertThat(response.userId()).isEqualTo(1L);
                assertThat(response.fullName()).isEqualTo("Alice");
                assertThat(response.email()).isEqualTo("alice@example.com");
                assertThat(response.role()).isEqualTo("DONOR");
                verify(registrationOtpService).verifyOtp("alice@example.com", "123456");
                verify(registrationOtpService).clearOtp("alice@example.com");
                verify(emailService, never()).sendVerificationEmail(any(User.class), anyString());
                verifyNoInteractions(ngoRepository);
        }

        @Test
        void register_ngoRoleAlsoCreatesNgoEntity() {
                RegisterRequest req = new RegisterRequest(
                                "NGO Org", "ngo@org.com", "secret", Role.NGO, null, "123456");
                User savedUser = User.builder()
                                .id(2L)
                                .fullName("NGO Org")
                                .email("ngo@org.com")
                                .password("enc")
                                .role(Role.NGO)
                                .emailVerified(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                when(userRepository.existsByEmail("ngo@org.com")).thenReturn(false);
                when(passwordEncoder.encode("secret")).thenReturn("enc");
                when(userRepository.save(any(User.class))).thenReturn(savedUser);
                when(jwtService.generateToken(any(User.class))).thenReturn("jwt-ngo-token");
                when(userRepository.findByRole(Role.ADMIN)).thenReturn(java.util.List.of(
                                User.builder().id(99L).email("admin@example.com").role(Role.ADMIN).build()));
                when(ngoRepository.save(any(Ngo.class))).thenAnswer(inv -> inv.getArgument(0));

                LoginResponse response = authService.register(req, null);

                verify(ngoRepository).save(any(Ngo.class));
                ArgumentCaptor<Ngo> ngoCaptor = ArgumentCaptor.forClass(Ngo.class);
                verify(ngoRepository).save(ngoCaptor.capture());
                assertThat(ngoCaptor.getValue().getStatus()).isEqualTo(NgoStatus.PENDING);
                assertThat(ngoCaptor.getValue().getUser()).isNotNull();
                assertThat(ngoCaptor.getValue().getUser().getEmail()).isEqualTo("ngo@org.com");
                assertThat(ngoCaptor.getValue().getUser().getRole()).isEqualTo(Role.NGO);
                assertThat(response.token()).isEqualTo("jwt-ngo-token");
                assertThat(response.userId()).isEqualTo(2L);
                assertThat(response.role()).isEqualTo("NGO");
                verify(emailService).sendNgoApplicationAlert(any(User.class), any(Ngo.class));
                verify(registrationOtpService).verifyOtp("ngo@org.com", "123456");
                verify(registrationOtpService).clearOtp("ngo@org.com");
        }

        @Test
        void register_duplicateEmail_throwsRuntimeException() {
                RegisterRequest req = new RegisterRequest(
                                "Alice", "alice@example.com", "pw", Role.DONOR, null, "123456");
                when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

                assertThatThrownBy(() -> authService.register(req, null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Email already registered");
        }

        @Test
        void sendRegistrationOtp_unregisteredEmail_sendsPreRegistrationOtp() {
                when(userRepository.existsByEmail("register@new.com")).thenReturn(false);
                when(registrationOtpService.generateAndStoreOtp("register@new.com")).thenReturn("987654");

                authService.sendRegistrationOtp("register@new.com");

                verify(emailService).sendPreRegistrationOtpEmail("register@new.com", "987654");
        }

        // ─── verifyEmail ─────────────────────────────────────────────────────────

        @Test
        void verifyEmail_throwsUnsupportedBecauseOnlyOtpIsAllowed() {
                assertThatThrownBy(() -> authService.verifyEmail("abc-token"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Link-based verification is not supported");
        }

        @Test
        void resendVerification_unverifiedUser_sendsOtpEmailAndSetsFields() {
                User user = User.builder()
                                .id(1L)
                                .email("otp@example.com")
                                .fullName("Otp User")
                                .emailVerified(false)
                                .build();
                when(userRepository.findByEmail("otp@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                authService.resendVerification("otp@example.com");

                assertThat(user.getEmailVerificationOtp()).isNotNull();
                assertThat(user.getEmailVerificationOtpExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));
                verify(emailService).sendVerificationOtpEmail(eq(user), anyString());
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
                assertThat(resp.userId()).isEqualTo(1L);
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

        // ─── OTP verification ─────────────────────────────────────────────────────

        @Test
        void login_firebaseBackedAccountWithoutPassword_throwsBadCredentialsException() {
                User user = User.builder()
                                .id(1L)
                                .email("firebase@example.com")
                                .firebaseUid("firebase-uid")
                                .password(null)
                                .role(Role.DONOR)
                                .build();
                when(userRepository.findByEmail("firebase@example.com")).thenReturn(Optional.of(user));

                assertThatThrownBy(() -> authService.login(new LoginRequest("firebase@example.com", "pw")))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessageContaining("Firebase sign-in");
        }

        @Test
        void sendOtp_unverifiedUser_sendsOtpEmailAndSetsFields() {
                User user = User.builder()
                                .id(1L)
                                .email("otp@example.com")
                                .fullName("Otp User")
                                .emailVerified(false)
                                .build();
                when(userRepository.findByEmail("otp@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                authService.sendOtp("otp@example.com");

                assertThat(user.getEmailVerificationOtp()).isNotNull();
                assertThat(user.getEmailVerificationOtpExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));
                verify(emailService).sendVerificationOtpEmail(eq(user), anyString());
        }

        @Test
        void sendOtp_userNotFound_throwsRuntimeException() {
                when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.sendOtp("missing@example.com"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("No account found with that email");
        }

        @Test
        void verifyOtp_validCode_marksUserVerifiedAndClearsOtp() {
                User user = User.builder()
                                .id(1L)
                                .email("otp@example.com")
                                .emailVerificationOtp("123456")
                                .emailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(5))
                                .emailVerificationOtpAttempts(0)
                                .emailVerified(false)
                                .build();
                when(userRepository.findByEmail("otp@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                authService.verifyOtp("otp@example.com", "123456");

                assertThat(user.isEmailVerified()).isTrue();
                assertThat(user.getEmailVerificationOtp()).isNull();
                assertThat(user.getEmailVerificationOtpExpiresAt()).isNull();
                assertThat(user.getEmailVerificationOtpAttempts()).isZero();
        }

        @Test
        void verifyOtp_wrongCode_incrementsAttemptsAndThrows() {
                User user = User.builder()
                                .id(1L)
                                .email("otp@example.com")
                                .emailVerificationOtp("123456")
                                .emailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(5))
                                .emailVerificationOtpAttempts(0)
                                .emailVerified(false)
                                .build();
                when(userRepository.findByEmail("otp@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatThrownBy(() -> authService.verifyOtp("otp@example.com", "000000"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Invalid or expired code");

                assertThat(user.getEmailVerificationOtpAttempts()).isEqualTo(1);
        }

        @Test
        void verifyOtp_expiredCode_incrementsAttemptsAndThrows() {
                User user = User.builder()
                                .id(1L)
                                .email("otp@example.com")
                                .emailVerificationOtp("123456")
                                .emailVerificationOtpExpiresAt(LocalDateTime.now().minusMinutes(1))
                                .emailVerificationOtpAttempts(0)
                                .emailVerified(false)
                                .build();
                when(userRepository.findByEmail("otp@example.com")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatThrownBy(() -> authService.verifyOtp("otp@example.com", "123456"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Invalid or expired code");

                assertThat(user.getEmailVerificationOtpAttempts()).isEqualTo(1);
        }

}
