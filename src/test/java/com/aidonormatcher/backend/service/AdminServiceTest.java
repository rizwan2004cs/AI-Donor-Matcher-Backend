package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.*;
import com.aidonormatcher.backend.repository.*;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private NgoRepository ngoRepository;
    @Mock private NeedRepository needRepository;
    @Mock private PledgeRepository pledgeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private TrustScoreService trustScoreService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AdminService adminService;

    private Ngo ngo;
    private Need need;

    @BeforeEach
    void setUp() {
        ngo = Ngo.builder().id(1L).name("Test NGO").contactEmail("ngo@org.com")
                .status(NgoStatus.PENDING).build();
        need = Need.builder().id(100L).ngo(ngo).itemName("Food")
                .status(NeedStatus.OPEN).quantityRequired(10).quantityPledged(0).build();
    }

    // ─── approveNgo ──────────────────────────────────────────────────────────

    @Test
    void approveNgo_setsApprovedStatusAndRecalculatesTrust() {
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));

        adminService.approveNgo(1L);

        assertThat(ngo.getStatus()).isEqualTo(NgoStatus.APPROVED);
        assertThat(ngo.getVerifiedAt()).isNotNull();
        verify(ngoRepository).save(ngo);
        verify(trustScoreService).recalculate(ngo);
        verify(emailService).sendNgoApprovedEmail(ngo);
    }

    @Test
    void approveNgo_notFound_throwsRuntimeException() {
        when(ngoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.approveNgo(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NGO not found");
    }

    // ─── rejectNgo ───────────────────────────────────────────────────────────

    @Test
    void rejectNgo_setsRejectionReasonAndEmailsNgo() {
        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));

        adminService.rejectNgo(1L, "Missing documents");

        assertThat(ngo.getRejectionReason()).isEqualTo("Missing documents");
        verify(ngoRepository).save(ngo);
        verify(emailService).sendNgoRejectedEmail(ngo, "Missing documents");
    }

    // ─── suspendNgo ──────────────────────────────────────────────────────────

    @Test
    void suspendNgo_setsSuspendedAndCancelsActivePledges() {
        ngo.setStatus(NgoStatus.APPROVED);
        User donor = User.builder().id(2L).email("donor@example.com").build();
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(1).status(PledgeStatus.ACTIVE).build();

        when(ngoRepository.findById(1L)).thenReturn(Optional.of(ngo));
        when(needRepository.findByNgoAndStatusIn(eq(ngo), anyList()))
                .thenReturn(List.of(need));
        when(pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE))
                .thenReturn(List.of(pledge));

        adminService.suspendNgo(1L);

        assertThat(ngo.getStatus()).isEqualTo(NgoStatus.SUSPENDED);
        assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.CANCELLED);
        assertThat(need.getStatus()).isEqualTo(NeedStatus.EXPIRED);
        verify(emailService).sendNgoSuspendedEmail(ngo);
    }

    // ─── removeNeed ──────────────────────────────────────────────────────────

    @Test
    void removeNeed_deletesNeedAndCancelsActivePledges() {
        User donor = User.builder().id(2L).build();
        Pledge pledge = Pledge.builder().id(200L).donor(donor).need(need)
                .quantity(1).status(PledgeStatus.ACTIVE).build();

        when(needRepository.findById(100L)).thenReturn(Optional.of(need));
        when(pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE))
                .thenReturn(List.of(pledge));

        adminService.removeNeed(100L);

        assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.CANCELLED);
        verify(needRepository).delete(need);
    }

    // ─── editNeed ────────────────────────────────────────────────────────────

    @Test
    void editNeed_updatesNeedFields() {
        NeedRequest req = new NeedRequest(NeedCategory.FOOD, "Rice", "White rice",
                20, UrgencyLevel.NORMAL, LocalDate.now().plusDays(14));

        when(needRepository.findById(100L)).thenReturn(Optional.of(need));
        when(needRepository.save(any(Need.class))).thenAnswer(inv -> inv.getArgument(0));

        Need updated = adminService.editNeed(100L, req);

        assertThat(updated.getItemName()).isEqualTo("Rice");
        assertThat(updated.getQuantityRequired()).isEqualTo(20);
    }

    // ─── getStats ────────────────────────────────────────────────────────────

    @Test
    void getStats_returnsMapWithAllKeys() {
        when(userRepository.count()).thenReturn(10L);
        when(ngoRepository.count()).thenReturn(5L);
        when(ngoRepository.findByStatus(NgoStatus.PENDING)).thenReturn(List.of(ngo));
        when(needRepository.count()).thenReturn(20L);
        when(pledgeRepository.count()).thenReturn(30L);
        when(reportRepository.count()).thenReturn(2L);
        when(needRepository.countByStatusIn(anyList())).thenReturn(12L);
        when(pledgeRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(4L);
        when(needRepository.countByFulfilledAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(6L);

        Map<String, Object> stats = adminService.getStats();

        assertThat(stats).containsKeys("totalUsers", "totalNgos", "pendingNgos",
                "totalNeeds", "totalPledges", "totalReports",
                "activeNeeds", "pledgesToday", "fulfillmentsThisMonth");
        assertThat(stats.get("totalUsers")).isEqualTo(10L);
        assertThat(stats.get("activeNeeds")).isEqualTo(12L);
    }
}
