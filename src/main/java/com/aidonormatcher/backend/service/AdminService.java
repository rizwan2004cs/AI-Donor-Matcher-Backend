package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.AdminNgoSummaryResponse;
import com.aidonormatcher.backend.dto.AdminReportSummaryResponse;
import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.entity.*;
import com.aidonormatcher.backend.enums.*;
import com.aidonormatcher.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final Sort NGO_LIST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final NgoRepository ngoRepository;
    private final NeedRepository needRepository;
    private final PledgeRepository pledgeRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final TrustScoreService trustScoreService;
    private final EmailService emailService;
    private final NeedService needService;

    public List<AdminNgoSummaryResponse> getPendingNgos(Integer limit) {
        if (limit == null || limit <= 0) {
            return ngoRepository.findByStatus(NgoStatus.PENDING).stream()
                    .map(this::toAdminNgoSummary)
                    .toList();
        }
        return ngoRepository.findByStatus(
                NgoStatus.PENDING,
                PageRequest.of(0, limit, NGO_LIST_SORT)
        ).getContent().stream()
                .map(this::toAdminNgoSummary)
                .toList();
    }

    public List<AdminNgoSummaryResponse> getAllNgos(Integer limit) {
        if (limit == null || limit <= 0) {
            return ngoRepository.findAll(NGO_LIST_SORT).stream()
                    .map(this::toAdminNgoSummary)
                    .toList();
        }
        return ngoRepository.findAll(PageRequest.of(0, limit, NGO_LIST_SORT)).getContent().stream()
                .map(this::toAdminNgoSummary)
                .toList();
    }

    @Transactional
    public void approveNgo(Long ngoId) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));
        ngo.setStatus(NgoStatus.APPROVED);
        ngo.setVerifiedAt(LocalDateTime.now());
        ngoRepository.save(ngo);
        trustScoreService.recalculate(ngo);
        emailService.sendNgoApprovedEmail(ngo);
    }

    @Transactional
    public void rejectNgo(Long ngoId, String reason) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));
        ngo.setRejectionReason(reason);
        ngoRepository.save(ngo);
        emailService.sendNgoRejectedEmail(ngo, reason);
    }

    @Transactional
    public void suspendNgo(Long ngoId) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));

        // 1. Set status to SUSPENDED
        ngo.setStatus(NgoStatus.SUSPENDED);
        ngoRepository.save(ngo);

        // 2. Get all active needs for this NGO
        List<Need> activeNeeds = needRepository.findByNgoAndStatusIn(ngo,
                List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));

        for (Need need : activeNeeds) {
            // 3. Cancel all active pledges on each need
            List<Pledge> pledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);
            for (Pledge pledge : pledges) {
                pledge.setStatus(PledgeStatus.CANCELLED);
                pledgeRepository.save(pledge);
                // 4. Email each affected donor
                emailService.sendPledgeCancelledByDonorEmail(ngo, pledge);
            }
            // 5. Close the need
            need.setStatus(NeedStatus.EXPIRED);
            needRepository.save(need);
        }

        emailService.sendNgoSuspendedEmail(ngo);
    }

    @Transactional
    public void removeNeed(Long needId) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));
        List<Pledge> activePledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);

        for (Pledge pledge : activePledges) {
            pledge.setStatus(PledgeStatus.CANCELLED);
            pledgeRepository.save(pledge);
            emailService.sendPledgeCancelledByDonorEmail(need.getNgo(), pledge);
        }

        needRepository.delete(need);
    }

    @Transactional
    public Need editNeed(Long needId, com.aidonormatcher.backend.dto.NeedRequest req) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));
        need.setCategory(req.category());
        need.setItemName(req.itemName());
        need.setDescription(req.description());
        need.setQuantityRequired(req.quantityRequired());
        need.setUrgency(req.urgency());
        need.setExpiryDate(req.expiryDate());
        return needRepository.save(need);
    }

    public List<AdminReportSummaryResponse> getReports(Integer limit) {
        if (limit == null || limit <= 0) {
            return reportRepository.findAllByOrderByReportedAtDesc().stream()
                    .map(this::toAdminReportSummary)
                    .toList();
        }
        return reportRepository.findAllByOrderByReportedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toAdminReportSummary)
                .toList();
    }

    public List<NeedDetailResponse> getNgoNeeds(Long ngoId) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));
        return needRepository.findByNgo(ngo).stream()
                .map(needService::toNeedDetailResponse)
                .toList();
    }

    private AdminNgoSummaryResponse toAdminNgoSummary(Ngo ngo) {
        User user = ngo.getUser();
        String email = ngo.getContactEmail();
        if ((email == null || email.isBlank()) && user != null) {
            email = user.getEmail();
        }

        return new AdminNgoSummaryResponse(
                ngo.getId(),
                user != null ? user.getId() : null,
                ngo.getName(),
                email,
                ngo.getAddress(),
                ngo.getPhotoUrl(),
                ngo.getDocumentUrl(),
                ngo.getStatus() != null ? ngo.getStatus().name() : null,
                ngo.getCategoryOfWork() != null ? ngo.getCategoryOfWork().name() : null,
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : null,
                ngo.getRejectionReason(),
                ngo.getVerifiedAt(),
                ngo.getCreatedAt()
        );
    }

    private AdminReportSummaryResponse toAdminReportSummary(Report report) {
        Ngo ngo = report.getNgo();
        User reporter = report.getReporter();

        return new AdminReportSummaryResponse(
                report.getId(),
                ngo != null ? ngo.getId() : null,
                ngo != null ? ngo.getName() : null,
                ngo != null && ngo.getStatus() != null ? ngo.getStatus().name() : null,
                report.getReason(),
                reporter != null ? reporter.getEmail() : null,
                report.getReportedAt()
        );
    }

    public Map<String, Object> getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalNgos", ngoRepository.count());
        stats.put("pendingNgos", ngoRepository.findByStatus(NgoStatus.PENDING).size());
        stats.put("approvedNgos", ngoRepository.findByStatus(NgoStatus.APPROVED).size());
        stats.put("suspendedNgos", ngoRepository.findByStatus(NgoStatus.SUSPENDED).size());
        stats.put("totalNeeds", needRepository.count());
        stats.put("totalPledges", pledgeRepository.count());
        stats.put("totalReports", reportRepository.count());
        stats.put("activeNeeds", needRepository.countByStatusIn(
                List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED)));
        stats.put("pledgesToday", pledgeRepository.countByCreatedAtBetween(startOfToday, startOfTomorrow));
        stats.put("fulfillmentsThisMonth", needRepository.countByFulfilledAtBetween(startOfMonth, startOfTomorrow));
        return stats;
    }
}
