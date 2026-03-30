package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.dto.NgoDetailResponse;
import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.GeocodingService.GeocodedPoint;
import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.enums.UrgencyLevel;
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
import java.util.List;

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
    @Mock private GeocodingService geocodingService;
    @Mock private NeedService needService;

    @InjectMocks
    private NgoService ngoService;

    private User user;
    private Ngo ngo;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("ngo@org.com").role(Role.NGO).build();
        ngo = Ngo.builder().id(1L).user(user).name("Test NGO")
                .address("123 Hope Street, Nellore")
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

    @Test
    void updateProfile_addressProvided_geocodesAndStoresCoordinates() {
        NgoProfileRequest req = new NgoProfileRequest(
                "Updated NGO", "123 Hope Street", "updated@ngo.com",
                "0987654321", "An updated description that is at least fifty characters long here.",
                null);

        when(userRepository.findByEmail("ngo@org.com")).thenReturn(Optional.of(user));
        when(ngoRepository.findByUser(user)).thenReturn(Optional.of(ngo));
        when(ngoRepository.save(any(Ngo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(geocodingService.geocode("123 Hope Street")).thenReturn(new GeocodedPoint(6.9271, 79.8612));

        Ngo updated = ngoService.updateProfile("ngo@org.com", req);

        assertThat(updated.getLat()).isEqualTo(6.9271);
        assertThat(updated.getLng()).isEqualTo(79.8612);
        verify(geocodingService).geocode("123 Hope Street");
    }

    @Test
    void updateProfile_geocodingFailure_throwsRuntimeExceptionAndDoesNotSave() {
        NgoProfileRequest req = new NgoProfileRequest(
                "Updated NGO", "Unknown place", "updated@ngo.com",
                "0987654321", "An updated description that is at least fifty characters long here.",
                null);

        when(userRepository.findByEmail("ngo@org.com")).thenReturn(Optional.of(user));
        when(ngoRepository.findByUser(user)).thenReturn(Optional.of(ngo));
        when(geocodingService.geocode("Unknown place"))
                .thenThrow(new RuntimeException("Unable to geocode the provided NGO address. Please enter a more specific address."));

        assertThatThrownBy(() -> ngoService.updateProfile("ngo@org.com", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to geocode");

        verify(ngoRepository, never()).save(any(Ngo.class));
    }

    // ─── getNgoById ──────────────────────────────────────────────────────────

    @Test
    void getNgoById_returnsNgoDetailWithActiveNeeds() {
        Need need = Need.builder()
                .id(10L)
                .ngo(ngo)
                .itemName("Rice")
                .quantityRequired(10)
                .quantityPledged(2)
                .category(NeedCategory.FOOD)
                .urgency(UrgencyLevel.NORMAL)
                .status(NeedStatus.OPEN)
                .build();
        NeedDetailResponse detail = new NeedDetailResponse(
                10L, 1L, "Test NGO", "123 Hope Street, Nellore", null, 0, null,
                "FOOD", "Rice", "Rice packs", 10, 2, 8,
                "NORMAL", null, "OPEN", null);

        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));
        when(needRepository.findByNgoAndStatusIn(eq(ngo), anyList())).thenReturn(List.of(need));
        when(needService.toNeedDetailResponse(need)).thenReturn(detail);

        NgoDetailResponse result = ngoService.getNgoById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.address()).isEqualTo("123 Hope Street, Nellore");
        assertThat(result.activeNeeds()).containsExactly(detail);
    }

    @Test
    void getNgoById_notFound_throwsRuntimeException() {
        when(ngoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ngoService.getNgoById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NGO not found");
    }

    @Test
    void discoverNgos_withCoordinates_mapsNearbyRowsUsingNgoIds() {
        ngo.setLat(14.4493717);
        ngo.setLng(79.9873763);
        ngo.setTrustScore(56);
        Need need = Need.builder()
                .id(10L)
                .ngo(ngo)
                .itemName("Rice")
                .quantityRequired(10)
                .quantityPledged(2)
                .category(NeedCategory.FOOD)
                .urgency(UrgencyLevel.NORMAL)
                .status(NeedStatus.OPEN)
                .build();

        when(ngoRepository.findNearby(14.44, 79.98, 50.0, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4.2d}));
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));
        when(needRepository.findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
                eq(ngo), anyList())).thenReturn(need);

        List<NgoDiscoveryDTO> results = ngoService.discoverNgos(14.44, 79.98, 50.0, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(1L);
        assertThat(results.getFirst().address()).isEqualTo("123 Hope Street, Nellore");
        assertThat(results.getFirst().distanceKm()).isEqualTo(4.2d);
        assertThat(results.getFirst().name()).isEqualTo("Test NGO");
    }
}
