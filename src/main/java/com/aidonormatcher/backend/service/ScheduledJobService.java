package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.PledgeStatus;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.PledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledJobService {

    private final PledgeRepository pledgeRepository;
    private final NeedRepository needRepository;
    private final NeedService needService;
    private final EmailService emailService;

    /**
     * Runs every hour. Expires any ACTIVE pledge older than 48 hours.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void expireOldPledges() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<Pledge> expired = pledgeRepository
                .findByStatusAndCreatedAtBefore(PledgeStatus.ACTIVE, cutoff);

        for (Pledge pledge : expired) {
            pledge.setStatus(PledgeStatus.EXPIRED);
            pledgeRepository.save(pledge);

            Need need = pledge.getNeed();
            need.setQuantityPledged(need.getQuantityPledged() - pledge.getQuantity());
            needService.recalculateStatus(need);

            emailService.sendPledgeExpiredEmail(pledge.getDonor(), pledge);
        }
    }

    /**
     * Runs daily at midnight. Closes expired needs and warns about upcoming expiry.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processNeedExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(3);

        // Warn NGOs about needs expiring in 3 days
        List<Need> expiringWarnings = needRepository
                .findByExpiryDateAndStatusIn(warningDate,
                        List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));
        for (Need need : expiringWarnings) {
            emailService.sendNeedExpiryWarning(need.getNgo(), need);
        }

        // Auto-close needs whose expiry date has passed
        List<Need> expiredNeeds = needRepository
                .findByExpiryDateBeforeAndStatusIn(today,
                        List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));
        for (Need need : expiredNeeds) {
            // Cancel active pledges
            List<Pledge> activePledges = pledgeRepository.findByNeedAndStatus(need, PledgeStatus.ACTIVE);
            for (Pledge pledge : activePledges) {
                pledge.setStatus(PledgeStatus.CANCELLED);
                pledgeRepository.save(pledge);
            }
            need.setStatus(NeedStatus.EXPIRED);
            needRepository.save(need);
            emailService.sendNeedAutoExpiredEmail(need.getNgo(), need);
        }
    }
}
