package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private NgoRepository ngoRepository;

    @Mock
    private NeedRepository needRepository;

    @InjectMocks
    private TrustScoreService trustScoreService;

    private Ngo ngo;

    @BeforeEach
    void setUp() {
        ngo = Ngo.builder()
                .id(1L)
                .status(NgoStatus.APPROVED)
                .name("Food Bank")
                .address("123 Main St")
                .contactEmail("food@bank.org")
                .contactPhone("0123456789")
                .description("A detailed description of our food bank that is at least fifty characters long.")
                .categoryOfWork(com.aidonormatcher.backend.enums.NeedCategory.FOOD)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    // --- Approved NGO base score (+40) ---

    @Test
    void recalculate_approvedNgoWithNoNeeds_scoreIs40Plus() {
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(0L);

        trustScoreService.recalculate(ngo);

        // 40 (approved) + 20 (all 6 profile fields complete) = 60 → ESTABLISHED
        assertThat(ngo.getTrustScore()).isEqualTo(60);
        assertThat(ngo.getTrustTier()).isEqualTo(TrustTier.ESTABLISHED);
        verify(ngoRepository).save(ngo);
    }

    @Test
    void recalculate_pendingNgoFullProfile_scoreIs20() {
        ngo.setStatus(NgoStatus.PENDING);
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(0L);

        trustScoreService.recalculate(ngo);

        // 0 (not approved) + 20 (full profile) = 20 → NEW
        assertThat(ngo.getTrustScore()).isEqualTo(20);
        assertThat(ngo.getTrustTier()).isEqualTo(TrustTier.NEW);
    }

    // --- Profile completeness scoring ---

    @Test
    void recalculate_emptyProfile_noProfilePoints() {
        Ngo sparse = Ngo.builder()
                .id(2L)
                .status(NgoStatus.PENDING)
                .lastActivityAt(LocalDateTime.now())
                .build();
        when(needRepository.countByNgoAndStatus(sparse, NeedStatus.FULFILLED)).thenReturn(0L);

        trustScoreService.recalculate(sparse);

        assertThat(sparse.getTrustScore()).isEqualTo(0);
        assertThat(sparse.getTrustTier()).isEqualTo(TrustTier.NEW);
    }

    // --- Fulfillment bonus (+2 per need, capped at 30) ---

    @Test
    void recalculate_fulfilledNeedsAddScore() {
        // 5 fulfilled needs → +10 bonus
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(5L);

        trustScoreService.recalculate(ngo);

        // 40 + 20 + 10 = 70 → TRUSTED
        assertThat(ngo.getTrustScore()).isEqualTo(70);
        assertThat(ngo.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    }

    @Test
    void recalculate_moreThan15FulfilledNeeds_cappedAtPlus30() {
        // 20 fulfilled needs → capped at +30
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(20L);

        trustScoreService.recalculate(ngo);

        // 40 + 20 + 30 (capped) = 90
        assertThat(ngo.getTrustScore()).isEqualTo(90);
        assertThat(ngo.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    }

    // --- Inactivity penalty (-10) ---

    @Test
    void recalculate_inactiveFor60DaysPenalisesScore() {
        ngo.setLastActivityAt(LocalDateTime.now().minusDays(61));
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(0L);

        trustScoreService.recalculate(ngo);

        // 40 + 20 - 10 = 50 → ESTABLISHED
        assertThat(ngo.getTrustScore()).isEqualTo(50);
    }

    // --- Score clamping ---

    @Test
    void recalculate_scoreIsClampedAboveZero() {
        Ngo sparse = Ngo.builder()
                .id(3L)
                .status(NgoStatus.PENDING)
                .lastActivityAt(LocalDateTime.now().minusDays(90))
                .build();
        when(needRepository.countByNgoAndStatus(sparse, NeedStatus.FULFILLED)).thenReturn(0L);

        trustScoreService.recalculate(sparse);

        assertThat(sparse.getTrustScore()).isGreaterThanOrEqualTo(0);
    }

    // --- Tier boundary checks ---

    @Test
    void recalculate_scoreOf70SetsTrustToTrusted() {
        when(needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED)).thenReturn(5L);

        trustScoreService.recalculate(ngo);

        assertThat(ngo.getTrustScore()).isGreaterThanOrEqualTo(70);
        assertThat(ngo.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    }

    // --- Helpers ---

    private Need buildFulfilledNeed() {
        return Need.builder()
                .id(99L)
                .ngo(ngo)
                .status(NeedStatus.FULFILLED)
                .itemName("Food")
                .build();
    }
}
