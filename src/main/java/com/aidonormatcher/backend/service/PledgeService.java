package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.IncomingPledgeResponse;
import com.aidonormatcher.backend.dto.PledgeRequest;
import com.aidonormatcher.backend.dto.PledgeDetailResponse;
import com.aidonormatcher.backend.dto.PledgeResponse;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.enums.PledgeStatus;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.PledgeRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PledgeService {

    private final PledgeRepository pledgeRepository;
    private final NeedRepository needRepository;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final NeedService needService;
    private final EmailService emailService;

    @Transactional
    public PledgeResponse createPledge(PledgeRequest req, Long donorId) {
        // 1. Load donor — verify email verification
        User donor = userRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found."));
        if (!donor.isEmailVerified()) {
            throw new RuntimeException("Email not verified.");
        }

        // 2. Load need with pessimistic write lock
        Need need = needRepository.findByIdWithLock(req.needId());

        // 3. Validate: need must be OPEN or PARTIALLY_PLEDGED
        if (need.getStatus() == NeedStatus.FULLY_PLEDGED
                || need.getStatus() == NeedStatus.FULFILLED
                || need.getStatus() == NeedStatus.EXPIRED) {
            throw new RuntimeException("This need is no longer available for pledging.");
        }

        // 4. Validate: requested quantity does not exceed remaining
        int remaining = need.getQuantityRemaining();
        if (req.quantity() > remaining) {
            throw new RuntimeException("Quantity requested exceeds remaining: " + remaining);
        }

        // 5. Create pledge
        Pledge pledge = Pledge.builder()
                .donor(donor)
                .need(need)
                .quantity(req.quantity())
                .status(PledgeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();
        pledgeRepository.save(pledge);

        // 6. Update need quantity and recalculate status
        need.setQuantityPledged(need.getQuantityPledged() + req.quantity());
        needService.recalculateStatus(need);

        // 7. Update NGO last activity
        Ngo ngo = need.getNgo();
        ngo.setLastActivityAt(LocalDateTime.now());
        ngoRepository.save(ngo);

        // 8. Send confirmation email
        emailService.sendPledgeConfirmationEmail(donor, pledge, ngo);

        // 9. Return response with NGO coordinates
        return new PledgeResponse(
                pledge.getId(), ngo.getLat(), ngo.getLng(),
                ngo.getAddress(), ngo.getContactEmail(), pledge.getExpiresAt()
        );
    }

    @Transactional
    public void cancelPledge(Long pledgeId, Long donorId) {
        Pledge pledge = pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new RuntimeException("Pledge not found."));

        if (!pledge.getDonor().getId().equals(donorId)) {
            throw new RuntimeException("Unauthorized.");
        }

        if (pledge.getStatus() != PledgeStatus.ACTIVE) {
            throw new RuntimeException("Only active pledges can be cancelled.");
        }

        pledge.setStatus(PledgeStatus.CANCELLED);
        pledgeRepository.save(pledge);

        Need need = pledge.getNeed();
        need.setQuantityPledged(need.getQuantityPledged() - pledge.getQuantity());
        needService.recalculateStatus(need);

        emailService.sendPledgeCancelledByDonorEmail(need.getNgo(), pledge);
    }

    public Pledge getPledgeById(Long pledgeId) {
        return pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new RuntimeException("Pledge not found."));
    }

    public PledgeDetailResponse getPledgeDetails(Long pledgeId, Long donorId) {
        Pledge pledge = getPledgeById(pledgeId);
        if (!pledge.getDonor().getId().equals(donorId)) {
            throw new RuntimeException("Unauthorized.");
        }
        return toPledgeDetailResponse(pledge);
    }

    public List<Pledge> getActivePledges(Long donorId) {
        User donor = userRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found."));
        return pledgeRepository.findByDonorAndStatus(donor, PledgeStatus.ACTIVE);
    }

    public List<Pledge> getPledgeHistory(Long donorId) {
        User donor = userRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found."));
        return pledgeRepository.findByDonor(donor);
    }

    public List<IncomingPledgeResponse> getIncomingPledges(Long ngoUserId) {
        Ngo ngo = ngoRepository.findByUserId(ngoUserId)
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return pledgeRepository.findByNeedNgoIdAndStatusInOrderByCreatedAtDesc(
                        ngo.getId(), List.of(PledgeStatus.ACTIVE, PledgeStatus.FULFILLED))
                .stream()
                .map(this::toIncomingPledgeResponse)
                .toList();
    }

    @Transactional
    public IncomingPledgeResponse receivePledge(Long pledgeId, Long ngoUserId) {
        Pledge pledge = pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new RuntimeException("Pledge not found."));

        Need need = pledge.getNeed();
        Ngo ngo = need.getNgo();
        if (!ngo.getUser().getId().equals(ngoUserId)) {
            throw new RuntimeException("Unauthorized.");
        }

        if (pledge.getStatus() != PledgeStatus.ACTIVE) {
            throw new RuntimeException("Only active pledges can be marked as received.");
        }

        pledge.setStatus(PledgeStatus.FULFILLED);
        pledgeRepository.save(pledge);

        need.setQuantityReceived(need.getQuantityReceived() + pledge.getQuantity());
        needService.recalculateStatus(need);

        ngo.setLastActivityAt(LocalDateTime.now());
        ngoRepository.save(ngo);

        return toIncomingPledgeResponse(pledge);
    }

    private PledgeDetailResponse toPledgeDetailResponse(Pledge pledge) {
        Need need = pledge.getNeed();
        Ngo ngo = need.getNgo();
        return new PledgeDetailResponse(
                pledge.getId(),
                need.getId(),
                ngo.getId(),
                ngo.getName(),
                ngo.getPhotoUrl(),
                need.getItemName(),
                need.getCategory() != null ? need.getCategory().name() : null,
                pledge.getQuantity(),
                pledge.getStatus() != null ? pledge.getStatus().name() : null,
                pledge.getCreatedAt(),
                pledge.getExpiresAt(),
                ngo.getLat(),
                ngo.getLng(),
                ngo.getAddress(),
                ngo.getContactEmail()
        );
    }

    private IncomingPledgeResponse toIncomingPledgeResponse(Pledge pledge) {
        return new IncomingPledgeResponse(
                pledge.getId(),
                pledge.getNeed().getId(),
                pledge.getDonor().getFullName(),
                pledge.getDonor().getEmail(),
                pledge.getNeed().getItemName(),
                pledge.getNeed().getCategory() != null ? pledge.getNeed().getCategory().name() : null,
                pledge.getQuantity(),
                pledge.getStatus() != null ? pledge.getStatus().name() : null,
                pledge.getCreatedAt(),
                pledge.getExpiresAt(),
                pledge.getNeed().getQuantityRequired(),
                pledge.getNeed().getQuantityPledged(),
                pledge.getNeed().getQuantityReceived(),
                pledge.getNeed().getQuantityRemainingToReceive()
        );
    }
}
