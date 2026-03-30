package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.IncomingPledgeResponse;
import com.aidonormatcher.backend.dto.PledgeDetailResponse;
import com.aidonormatcher.backend.dto.PledgeRequest;
import com.aidonormatcher.backend.dto.PledgeResponse;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.PledgeStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.PledgeRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PledgeServiceTest {

    @Mock private PledgeRepository pledgeRepository;
    @Mock private NeedRepository needRepository;
    @Mock private UserRepository userRepository;
    @Mock private NgoRepository ngoRepository;
    @Mock private NeedService needService;
    @Mock private EmailService emailService;

    @InjectMocks
    private PledgeService pledgeService;

    private User donor;
    private Ngo ngo;
    private Need need;

    @BeforeEach
    void setUp() {
        donor = User.builder().id(1L).email("donor@example.com")
                .fullName("Alice Donor").role(Role.DONOR).emailVerified(true).build();
        User ngoUser = User.builder().id(10L).email("ngo@example.com")
                .role(Role.NGO).build();
        ngo = Ngo.builder().id(10L).user(ngoUser).name("Helping Hands")
                .contactEmail("ngo@org.com").address("123 St").photoUrl("https://cdn.example.com/ngo.jpg").build();
        need = Need.builder().id(50L).ngo(ngo).itemName("Food")
                .quantityRequired(10).quantityPledged(0).category(com.aidonormatcher.backend.enums.NeedCategory.FOOD)
                .status(NeedStatus.OPEN).build();
    }

    // ─── createPledge ────────────────────────────────────────────────────────

    @Test
    void createPledge_success_returnsResponseAndUpdatesNeed() {
        PledgeRequest req = new PledgeRequest(50L, 3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(needRepository.findByIdWithLock(50L)).thenReturn(need);
        when(pledgeRepository.save(any(Pledge.class))).thenAnswer(inv -> inv.getArgument(0));

        PledgeResponse resp = pledgeService.createPledge(req, 1L);

        assertThat(need.getQuantityPledged()).isEqualTo(3);
        verify(needService).recalculateStatus(need);
        verify(emailService).sendPledgeConfirmationEmail(eq(donor), any(Pledge.class), eq(ngo));
    }

    @Test
    void createPledge_emailNotVerified_throwsRuntimeException() {
        donor.setEmailVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(donor));

        assertThatThrownBy(() -> pledgeService.createPledge(new PledgeRequest(50L, 1), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    void createPledge_needFullyPledged_throwsRuntimeException() {
        need.setStatus(NeedStatus.FULLY_PLEDGED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(needRepository.findByIdWithLock(50L)).thenReturn(need);

        assertThatThrownBy(() -> pledgeService.createPledge(new PledgeRequest(50L, 1), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void createPledge_excessiveQuantity_throwsRuntimeException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(needRepository.findByIdWithLock(50L)).thenReturn(need);

        assertThatThrownBy(() -> pledgeService.createPledge(new PledgeRequest(50L, 99), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("exceeds remaining");
    }

    // ─── cancelPledge ────────────────────────────────────────────────────────

    @Test
    void cancelPledge_success_setsCancelledAndUpdateNeed() {
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(3).status(PledgeStatus.ACTIVE).build();
        need.setQuantityPledged(3);
        when(pledgeRepository.findById(200L)).thenReturn(Optional.of(pledge));

        pledgeService.cancelPledge(200L, 1L);

        assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.CANCELLED);
        assertThat(need.getQuantityPledged()).isEqualTo(0);
        verify(needService).recalculateStatus(need);
    }

    @Test
    void cancelPledge_unauthorized_throwsRuntimeException() {
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(3).status(PledgeStatus.ACTIVE).build();
        when(pledgeRepository.findById(200L)).thenReturn(Optional.of(pledge));

        assertThatThrownBy(() -> pledgeService.cancelPledge(200L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void cancelPledge_alreadyCancelled_throwsRuntimeException() {
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(3).status(PledgeStatus.CANCELLED).build();
        when(pledgeRepository.findById(200L)).thenReturn(Optional.of(pledge));

        assertThatThrownBy(() -> pledgeService.cancelPledge(200L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("active pledges");
    }

    // ─── getActivePledges ────────────────────────────────────────────────────

    @Test
    void getActivePledges_returnsDonorActivePledges() {
        Pledge pledge = Pledge.builder().id(1L).donor(donor).status(PledgeStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(pledgeRepository.findByDonorAndStatus(donor, PledgeStatus.ACTIVE))
                .thenReturn(List.of(pledge));

        List<Pledge> result = pledgeService.getActivePledges(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PledgeStatus.ACTIVE);
    }

    @Test
    void getPledgeDetails_authorizedDonor_returnsMappedResponse() {
        Pledge pledge = Pledge.builder()
                .id(200L)
                .donor(donor)
                .need(need)
                .quantity(3)
                .status(PledgeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();
        when(pledgeRepository.findById(200L)).thenReturn(Optional.of(pledge));

        PledgeDetailResponse response = pledgeService.getPledgeDetails(200L, 1L);

        assertThat(response.pledgeId()).isEqualTo(200L);
        assertThat(response.needId()).isEqualTo(50L);
        assertThat(response.ngoName()).isEqualTo("Helping Hands");
        assertThat(response.category()).isEqualTo("FOOD");
    }

    @Test
    void getPledgeDetails_otherDonor_throwsRuntimeException() {
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(3).status(PledgeStatus.ACTIVE).build();
        when(pledgeRepository.findById(200L)).thenReturn(Optional.of(pledge));

        assertThatThrownBy(() -> pledgeService.getPledgeDetails(200L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void getIncomingPledges_returnsActiveIncomingPledgeSummaries() {
        Pledge pledge = Pledge.builder()
                .id(200L)
                .donor(donor)
                .need(need)
                .quantity(3)
                .status(PledgeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        when(ngoRepository.findByUserId(10L)).thenReturn(Optional.of(ngo));
        when(pledgeRepository.findByNeedNgoIdAndStatusOrderByCreatedAtDesc(10L, PledgeStatus.ACTIVE))
                .thenReturn(List.of(pledge));

        List<IncomingPledgeResponse> responses = pledgeService.getIncomingPledges(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).donorName()).isEqualTo("Alice Donor");
        assertThat(responses.get(0).donorEmail()).isEqualTo("donor@example.com");
        assertThat(responses.get(0).itemName()).isEqualTo("Food");
    }
}
