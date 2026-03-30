package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedDetailResponse;
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
        if (need.getQuantityReceived() >= need.getQuantityRequired()) {
            need.setStatus(NeedStatus.FULFILLED);
            if (need.getFulfilledAt() == null) {
                need.setFulfilledAt(LocalDateTime.now());
            }
        } else if (need.getQuantityPledged() == 0) {
            need.setStatus(NeedStatus.OPEN);
            need.setFulfilledAt(null);
        } else if (need.getQuantityPledged() < need.getQuantityRequired()) {
            need.setStatus(NeedStatus.PARTIALLY_PLEDGED);
            need.setFulfilledAt(null);
        } else {
            need.setStatus(NeedStatus.FULLY_PLEDGED);
            need.setFulfilledAt(null);
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

        if (need.getQuantityReceived() < need.getQuantityRequired()) {
            throw new RuntimeException("Use pledge receipt updates until the received quantity reaches the full need total.");
        }

        need.setStatus(NeedStatus.FULFILLED);
        need.setFulfilledAt(LocalDateTime.now());
        needRepository.save(need);
    }

    public List<Need> getNeedsByNgo(Ngo ngo) {
        return needRepository.findByNgo(ngo);
    }

    public NeedDetailResponse getNeedDetail(Long needId) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found."));
        return toNeedDetailResponse(need);
    }

    public NeedDetailResponse toNeedDetailResponse(Need need) {
        Ngo ngo = need.getNgo();
        return new NeedDetailResponse(
                need.getId(),
                ngo.getId(),
                ngo.getName(),
                ngo.getAddress(),
                ngo.getPhotoUrl(),
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : null,
                need.getCategory() != null ? need.getCategory().name() : null,
                need.getItemName(),
                need.getDescription(),
                need.getQuantityRequired(),
                need.getQuantityPledged(),
                need.getQuantityRemaining(),
                need.getUrgency() != null ? need.getUrgency().name() : null,
                need.getExpiryDate(),
                need.getStatus() != null ? need.getStatus().name() : null,
                need.getCreatedAt()
        );
    }
}
