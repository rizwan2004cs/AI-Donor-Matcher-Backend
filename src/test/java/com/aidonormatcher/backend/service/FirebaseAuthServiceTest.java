package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.FirebaseRegisterRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthServiceTest {

    @Mock
    private FirebaseTokenService firebaseTokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NgoRepository ngoRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private FirebaseAuthService firebaseAuthService;

    @Test
    void login_linksExistingEmailToFirebaseAndReturnsSession() {
        User user = User.builder()
                .id(5L)
                .fullName("Mohammad Rizwan")
                .email("rizwan@example.com")
                .role(Role.DONOR)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(firebaseTokenService.verifyIdToken("firebase-token"))
                .thenReturn(new FirebaseUserInfo("firebase-uid", "rizwan@example.com", true, "Mohammad Rizwan"));
        when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rizwan@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = firebaseAuthService.login("firebase-token");

        assertThat(response.token()).isEqualTo("firebase-token");
        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.role()).isEqualTo("DONOR");
        assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid");
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void register_newNgoCreatesPendingNgoAndReturnsSession() {
        FirebaseRegisterRequest request = new FirebaseRegisterRequest("Helping Hands", Role.NGO, "Nellore");
        User savedUser = User.builder()
                .id(9L)
                .fullName("Helping Hands")
                .email("ngo@example.com")
                .firebaseUid("firebase-ngo")
                .role(Role.NGO)
                .emailVerified(true)
                .location("Nellore")
                .createdAt(LocalDateTime.now())
                .build();
        User admin = User.builder()
                .id(99L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        when(firebaseTokenService.verifyIdToken("firebase-token"))
                .thenReturn(new FirebaseUserInfo("firebase-ngo", "ngo@example.com", true, "Helping Hands"));
        when(userRepository.findByFirebaseUid("firebase-ngo")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ngo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(ngoRepository.findByUser(savedUser)).thenReturn(Optional.empty());
        when(ngoRepository.save(any(Ngo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        LoginResponse response = firebaseAuthService.register(request, null, "firebase-token");

        assertThat(response.token()).isEqualTo("firebase-token");
        assertThat(response.userId()).isEqualTo(9L);
        assertThat(response.email()).isEqualTo("ngo@example.com");
        assertThat(response.role()).isEqualTo("NGO");

        ArgumentCaptor<Ngo> ngoCaptor = ArgumentCaptor.forClass(Ngo.class);
        verify(ngoRepository).save(ngoCaptor.capture());
        Ngo savedNgo = ngoCaptor.getValue();
        assertThat(savedNgo.getStatus()).isEqualTo(NgoStatus.PENDING);
        assertThat(savedNgo.getUser()).isEqualTo(savedUser);
        verify(emailService).sendNgoApplicationAlert(admin, savedNgo);
    }

    @Test
    void register_existingEmailWithDifferentRole_throwsRuntimeException() {
        FirebaseRegisterRequest request = new FirebaseRegisterRequest("Helping Hands", Role.NGO, "Nellore");
        User existingUser = User.builder()
                .id(10L)
                .email("ngo@example.com")
                .role(Role.DONOR)
                .build();

        when(firebaseTokenService.verifyIdToken("firebase-token"))
                .thenReturn(new FirebaseUserInfo("firebase-ngo", "ngo@example.com", true, "Helping Hands"));
        when(userRepository.findByFirebaseUid("firebase-ngo")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ngo@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> firebaseAuthService.register(request, null, "firebase-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("role DONOR");

        verify(ngoRepository, never()).save(any(Ngo.class));
    }
}
