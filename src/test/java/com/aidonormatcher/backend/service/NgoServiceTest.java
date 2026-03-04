package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NgoServiceTest {

    @Mock private NgoRepository ngoRepository;
    @Mock private NeedRepository needRepository;
    @Mock private UserRepository userRepository;
    @Mock private TrustScoreService trustScoreService;

    @InjectMocks
    private NgoService ngoService;

    private User user;
    private Ngo ngo;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("ngo@org.com").role(Role.NGO).build();
        ngo = Ngo.builder().id(1L).user(user).name("Test NGO")
                .status(NgoStatus.APPROVED).build();
    }

    // ─── getMyProfile ────────────────────────────────────────────────────────

    @Test
    void getMyProfile_returnsNgoForAuthenticatedUser() {
        when(userRepository.findByEmail("ngo@org.com")).thenReturn(Optional.of(user));
        when(ngoRepository.findByUser(user)).thenReturn(Optional.of(ngo));

        Ngo result = ngoService.getMyProfile("ngo@org.com");

        assertThat(result).isEqualTo(ngo);
    }

    @Test
    void getMyProfile_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail("missing@org.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ngoService.getMyProfile("missing@org.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── updateProfile ───────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesFieldsAndSaves() {
        // address=null so geocoding is skipped
        NgoProfileRequest req = new NgoProfileRequest(
                "Updated NGO", null, "updated@ngo.com",
                "0987654321", "An updated description that is at least fifty characters long here.",
                null);

        when(userRepository.findByEmail("ngo@org.com")).thenReturn(Optional.of(user));
        when(ngoRepository.findByUser(user)).thenReturn(Optional.of(ngo));
        when(ngoRepository.save(any(Ngo.class))).thenAnswer(inv -> inv.getArgument(0));

        Ngo updated = ngoService.updateProfile("ngo@org.com", req);

        assertThat(updated.getName()).isEqualTo("Updated NGO");
        assertThat(updated.getContactEmail()).isEqualTo("updated@ngo.com");
        verify(trustScoreService).recalculate(ngo);
    }

    // ─── getNgoById ──────────────────────────────────────────────────────────

    @Test
    void getNgoById_returnsNgo() {
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));

        Ngo result = ngoService.getNgoById(1L);

        assertThat(result).isEqualTo(ngo);
    }

    @Test
    void getNgoById_notFound_throwsRuntimeException() {
        when(ngoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ngoService.getNgoById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NGO not found");
    }
}
