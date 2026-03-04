package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.PledgeStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.PledgeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

    @Mock private PledgeRepository pledgeRepository;
    @Mock private NeedRepository needRepository;
    @Mock private NeedService needService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ScheduledJobService scheduledJobService;

    private User donor;
    private Ngo ngo;
    private Need need;

    @BeforeEach
    void setUp() {
        donor = User.builder().id(1L).email("donor@example.com").role(Role.DONOR).build();
        ngo = Ngo.builder().id(10L).contactEmail("ngo@org.com").build();
        need = Need.builder().id(50L).ngo(ngo).itemName("Clothes")
                .quantityRequired(5).quantityPledged(2)
                .status(NeedStatus.PARTIALLY_PLEDGED).build();
    }

    // ─── expireOldPledges ────────────────────────────────────────────────────

    @Test
    void expireOldPledges_expiresStalePledgesAndUpdatesNeed() {
        Pledge oldPledge = Pledge.builder().id(1L).donor(donor).need(need)
                .quantity(2).status(PledgeStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusHours(49))
                .build();
        when(pledgeRepository.findByStatusAndCreatedAtBefore(eq(PledgeStatus.ACTIVE), any()))
                .thenReturn(List.of(oldPledge));

        scheduledJobService.expireOldPledges();

        assertThat(oldPledge.getStatus()).isEqualTo(PledgeStatus.EXPIRED);
        assertThat(need.getQuantityPledged()).isEqualTo(0);
        verify(needService).recalculateStatus(need);
        verify(emailService).sendPledgeExpiredEmail(donor, oldPledge);
    }

    @Test
    void expireOldPledges_noStalePledges_doesNothing() {
        when(pledgeRepository.findByStatusAndCreatedAtBefore(eq(PledgeStatus.ACTIVE), any()))
                .thenReturn(Collections.emptyList());

        scheduledJobService.expireOldPledges();

        verifyNoInteractions(needService, emailService);
    }

    // ─── processNeedExpiry ───────────────────────────────────────────────────

    @Test
    void processNeedExpiry_expiredNeedsSetToExpiredAndActivesPledgesCancelled() {
        Pledge activePledge = Pledge.builder().id(2L).donor(donor).need(need)
                .quantity(1).status(PledgeStatus.ACTIVE).build();
        need.setStatus(NeedStatus.OPEN);

        when(needRepository.findByExpiryDateAndStatusIn(any(LocalDate.class), anyList()))
                .thenReturn(Collections.emptyList());
        when(needRepository.findByExpiryDateBeforeAndStatusIn(any(LocalDate.class), anyList()))
                .thenReturn(List.of(need));
        when(pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE))
                .thenReturn(List.of(activePledge));

        scheduledJobService.processNeedExpiry();

        assertThat(activePledge.getStatus()).isEqualTo(PledgeStatus.CANCELLED);
        assertThat(need.getStatus()).isEqualTo(NeedStatus.EXPIRED);
        verify(emailService).sendNeedAutoExpiredEmail(ngo, need);
    }

    @Test
    void processNeedExpiry_warnsAboutNeedsExpiringSoon() {
        when(needRepository.findByExpiryDateAndStatusIn(any(LocalDate.class), anyList()))
                .thenReturn(List.of(need));
        when(needRepository.findByExpiryDateBeforeAndStatusIn(any(LocalDate.class), anyList()))
                .thenReturn(Collections.emptyList());

        scheduledJobService.processNeedExpiry();

        verify(emailService).sendNeedExpiryWarning(ngo, need);
    }
}
