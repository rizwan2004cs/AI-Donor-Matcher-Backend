package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.enums.UrgencyLevel;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeedServiceTest {

    @Mock private NeedRepository needRepository;
    @Mock private NgoRepository ngoRepository;

    @InjectMocks
    private NeedService needService;

    private User ngoUser;
    private Ngo ngo;
    private Need need;
    private NeedRequest needRequest;

    @BeforeEach
    void setUp() {
        ngoUser = User.builder().id(10L).email("ngo@org.com").role(Role.NGO).build();
        ngo = Ngo.builder().id(1L).user(ngoUser).status(NgoStatus.APPROVED).build();
        need = Need.builder()
                .id(100L).ngo(ngo).itemName("Blankets")
                .quantityRequired(10).quantityPledged(0)
                .status(NeedStatus.OPEN).createdAt(LocalDateTime.now())
                .build();
        needRequest = new NeedRequest(NeedCategory.CLOTHING, "Blankets", "Warm blankets",
                10, UrgencyLevel.URGENT, LocalDate.now().plusDays(30));
    }

    // ─── createNeed ──────────────────────────────────────────────────────────

    @Test
    void createNeed_success_savesAndReturnsNeed() {
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));
        when(needRepository.countByNgoAndStatusIn(eq(ngo), anyList())).thenReturn(0L);
        when(needRepository.save(any(Need.class))).thenAnswer(inv -> inv.getArgument(0));

        Need created = needService.createNeed(needRequest, 1L);

        assertThat(created.getItemName()).isEqualTo("Blankets");
        assertThat(created.getStatus()).isEqualTo(NeedStatus.OPEN);
        assertThat(created.getQuantityPledged()).isEqualTo(0);
        verify(needRepository).save(any(Need.class));
    }

    @Test
    void createNeed_ngoNotFound_throwsRuntimeException() {
        assertThatThrownBy(() -> needService.createNeed(needRequest, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NGO not found");
    }

    @Test
    void createNeed_exceedsMaxNeeds_throwsRuntimeException() {
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));
        when(needRepository.countByNgoAndStatusIn(eq(ngo), anyList())).thenReturn(5L);

        assertThatThrownBy(() -> needService.createNeed(needRequest, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Maximum 5");
    }

    // ─── updateNeed ──────────────────────────────────────────────────────────

    @Test
    void updateNeed_success_updatesFieldsAndSaves() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));
        when(needRepository.save(any(Need.class))).thenAnswer(inv -> inv.getArgument(0));

        Need updated = needService.updateNeed(100L, needRequest, 10L);

        assertThat(updated.getItemName()).isEqualTo("Blankets");
        verify(needRepository).save(need);
    }

    @Test
    void updateNeed_unauthorized_throwsRuntimeException() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        assertThatThrownBy(() -> needService.updateNeed(100L, needRequest, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void updateNeed_lockedNeed_throwsRuntimeException() {
        need.setQuantityPledged(3);
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        assertThatThrownBy(() -> needService.updateNeed(100L, needRequest, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("locked");
    }

    // ─── deleteNeed ──────────────────────────────────────────────────────────

    @Test
    void deleteNeed_success_deletesNeed() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        needService.deleteNeed(100L, 10L);

        verify(needRepository).delete(need);
    }

    @Test
    void deleteNeed_unauthorized_throwsRuntimeException() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        assertThatThrownBy(() -> needService.deleteNeed(100L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void deleteNeed_lockedNeed_throwsRuntimeException() {
        need.setQuantityPledged(2);
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        assertThatThrownBy(() -> needService.deleteNeed(100L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("locked");
    }

    // ─── recalculateStatus ───────────────────────────────────────────────────

    @Test
    void recalculateStatus_noPledged_setsOpen() {
        need.setQuantityRequired(10);
        need.setQuantityPledged(0);

        needService.recalculateStatus(need);

        assertThat(need.getStatus()).isEqualTo(NeedStatus.OPEN);
    }

    @Test
    void recalculateStatus_partialPledged_setsPartiallyPledged() {
        need.setQuantityRequired(10);
        need.setQuantityPledged(5);

        needService.recalculateStatus(need);

        assertThat(need.getStatus()).isEqualTo(NeedStatus.PARTIALLY_PLEDGED);
    }

    @Test
    void recalculateStatus_fullyPledged_setsFullyPledged() {
        need.setQuantityRequired(10);
        need.setQuantityPledged(10);

        needService.recalculateStatus(need);

        assertThat(need.getStatus()).isEqualTo(NeedStatus.FULLY_PLEDGED);
    }

    // ─── fulfillNeed ────────────────────────────────────────────────────────

    @Test
    void fulfillNeed_success_setsFulfilledStatus() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));
        when(needRepository.save(any(Need.class))).thenAnswer(inv -> inv.getArgument(0));

        needService.fulfillNeed(100L, 10L);

        assertThat(need.getStatus()).isEqualTo(NeedStatus.FULFILLED);
        assertThat(need.getFulfilledAt()).isNotNull();
    }

    @Test
    void fulfillNeed_unauthorized_throwsRuntimeException() {
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        assertThatThrownBy(() -> needService.fulfillNeed(100L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    // ─── getNeedsByNgo ───────────────────────────────────────────────────────

    @Test
    void getNeedsByNgo_delegatesToRepository() {
        when(needRepository.findByNgo(ngo)).thenReturn(List.of(need));

        List<Need> result = needService.getNeedsByNgo(ngo);

        assertThat(result).containsExactly(need);
    }

    @Test
    void getNeedDetail_returnsSafeMappedResponse() {
        ngo.setName("Helping Hands");
        ngo.setAddress("123 Main Street");
        ngo.setPhotoUrl("https://cdn.example.com/ngo.jpg");
        ngo.setTrustScore(88);
        ngo.setTrustTier(TrustTier.TRUSTED);
        need.setCategory(NeedCategory.CLOTHING);
        need.setUrgency(UrgencyLevel.URGENT);
        when(needRepository.findById(100L)).thenReturn(Optional.of(need));

        NeedDetailResponse response = needService.getNeedDetail(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.ngoName()).isEqualTo("Helping Hands");
        assertThat(response.ngoTrustTier()).isEqualTo("TRUSTED");
        assertThat(response.quantityRemaining()).isEqualTo(10);
        assertThat(response.category()).isEqualTo("CLOTHING");
    }
}
