package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NeedService {

    private final NeedRepository needRepository;
    private final NgoRepository ngoRepository;

    @Transactional
    public Need createNeed(NeedRequest req, Long ngoId) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));

        long activeCount = needRepository.countByNgoAndStatusIn(ngo,
                List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED, NeedStatus.FULLY_PLEDGED));
        if (activeCount >= 5) {
            throw new RuntimeException("Maximum 5 active needs reached.");
        }

        Need need = Need.builder()
                .ngo(ngo)
                .category(req.category())
                .itemName(req.itemName())
                .description(req.description())
                .quantityRequired(req.quantityRequired())
                .quantityPledged(0)
                .urgency(req.urgency())
                .expiryDate(req.expiryDate())
                .status(NeedStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        return needRepository.save(need);
    }

    @Transactional
    public Need updateNeed(Long needId, NeedRequest req, Long ngoUserId) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));

        if (!need.getNgo().getUser().getId().equals(ngoUserId)) {
            throw new RuntimeException("Unauthorized.");
        }

        if (need.getQuantityPledged() > 0) {
            throw new RuntimeException("This need is locked because it has active pledges.");
        }

        need.setCategory(req.category());
        need.setItemName(req.itemName());
        need.setDescription(req.description());
        need.setQuantityRequired(req.quantityRequired());
        need.setUrgency(req.urgency());
        need.setExpiryDate(req.expiryDate());

        return needRepository.save(need);
    }

    @Transactional
    public void deleteNeed(Long needId, Long ngoUserId) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));

        if (!need.getNgo().getUser().getId().equals(ngoUserId)) {
            throw new RuntimeException("Unauthorized.");
        }

        if (need.getQuantityPledged() > 0) {
            throw new RuntimeException("This need is locked because it has active pledges.");
        }

        needRepository.delete(need);
    }

    public void recalculateStatus(Need need) {
        int remaining = need.getQuantityRequired() - need.getQuantityPledged();
        if (need.getQuantityPledged() == 0) {
            need.setStatus(NeedStatus.OPEN);
        } else if (remaining > 0) {
            need.setStatus(NeedStatus.PARTIALLY_PLEDGED);
        } else {
            need.setStatus(NeedStatus.FULLY_PLEDGED);
        }
        needRepository.save(need);
    }

    @Transactional
    public void fulfillNeed(Long needId, Long ngoUserId) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));

        if (!need.getNgo().getUser().getId().equals(ngoUserId)) {
            throw new RuntimeException("Unauthorized.");
        }

        need.setStatus(NeedStatus.FULFILLED);
        need.setFulfilledAt(LocalDateTime.now());
        needRepository.save(need);
    }

    public List<Need> getNeedsByNgo(Ngo ngo) {
        return needRepository.findByNgo(ngo);
    }
}
