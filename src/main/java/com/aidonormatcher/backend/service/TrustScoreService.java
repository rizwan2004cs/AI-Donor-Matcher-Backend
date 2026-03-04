package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final NgoRepository ngoRepository;
    private final NeedRepository needRepository;

    public void recalculate(Ngo ngo) {
        int score = 0;

        // 1. Admin verification: +40
        if (ngo.getStatus() == NgoStatus.APPROVED) score += 40;

        // 2. Profile completeness: 0–20 (based on 6 required fields)
        int filledFields = 0;
        if (ngo.getName() != null && !ngo.getName().isBlank()) filledFields++;
        if (ngo.getAddress() != null && !ngo.getAddress().isBlank()) filledFields++;
        if (ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()) filledFields++;
        if (ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()) filledFields++;
        if (ngo.getDescription() != null && ngo.getDescription().length() >= 50) filledFields++;
        if (ngo.getCategoryOfWork() != null) filledFields++;
        score += (int) ((filledFields / 6.0) * 20);

        // 3. Fulfilled donations: +2 per fulfillment, capped at 30
        long fulfilledCount = needRepository.countByNgoAndStatus(ngo, NeedStatus.FULFILLED);
        score += (int) Math.min(30, fulfilledCount * 2);

        // 4. Activity recency penalty: -10 if no activity for 60+ days
        if (ngo.getLastActivityAt() != null) {
            long daysSinceActivity = ChronoUnit.DAYS.between(
                    ngo.getLastActivityAt(), LocalDateTime.now());
            if (daysSinceActivity > 60) score -= 10;
        }

        // Clamp to 0–100
        score = Math.max(0, Math.min(100, score));

        ngo.setTrustScore(score);
        ngo.setTrustTier(score >= 70 ? TrustTier.TRUSTED
                : score >= 40 ? TrustTier.ESTABLISHED
                : TrustTier.NEW);
        ngoRepository.save(ngo);
    }
}
