package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.dto.NgoDetailResponse;
import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedStatus;
import com.aidonormatcher.backend.repository.NeedRepository;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NgoService {

    private final NgoRepository ngoRepository;
    private final NeedRepository needRepository;
    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;
    private final GeocodingService geocodingService;
    private final NeedService needService;

    public Ngo getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        return ngoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
    }

    @Transactional
    public Ngo updateProfile(String email, NgoProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        Ngo ngo = ngoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));

        if (req.name() != null) ngo.setName(req.name());
        if (req.contactEmail() != null) ngo.setContactEmail(req.contactEmail());
        if (req.contactPhone() != null) ngo.setContactPhone(req.contactPhone());
        if (req.description() != null) ngo.setDescription(req.description());
        if (req.categoryOfWork() != null) ngo.setCategoryOfWork(req.categoryOfWork());

        if (req.address() != null) {
            applyAddressUpdate(ngo, req.address());
        }

        ngo.setProfileComplete(checkProfileComplete(ngo));

        ngoRepository.save(ngo);

        // Recalculate trust score after profile update
        trustScoreService.recalculate(ngo);

        return ngo;
    }

    public void updatePhotoUrl(String email, String url) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        Ngo ngo = ngoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        ngo.setPhotoUrl(url);
        ngoRepository.save(ngo);
    }

    public NgoDetailResponse getNgoById(Long id) {
        return ngoRepository.findById(id)
                .map(this::toNgoDetailResponse)
                .orElseThrow(() -> new RuntimeException("NGO not found."));
    }

    public List<NgoDiscoveryDTO> discoverNgos(Double lat, Double lng,
                                               Double radius, String category, String search) {
        // If lat/lng not provided, return all live NGOs
        if (lat == null || lng == null) {
            List<Ngo> ngos = ngoRepository.findAllLive(category, search);
            return ngos.stream().map(ngo -> {
                Need topNeed = needRepository
                        .findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
                                ngo, List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));
                return buildDiscoveryDTO(ngo, 0.0, topNeed);
            }).toList();
        }

        double rad = (radius != null) ? radius : 50.0; // default 50km
        List<Object[]> rows = ngoRepository.findNearby(lat, lng, rad, category, search);
        return rows.stream().map(row -> {
            Long ngoId = ((Number) row[0]).longValue();
            double distanceKm = ((Number) row[1]).doubleValue();
            Ngo ngo = ngoRepository.findById(ngoId)
                    .orElseThrow(() -> new RuntimeException("NGO not found."));

            Need topNeed = needRepository
                    .findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
                            ngo, List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));

            return buildDiscoveryDTO(ngo, distanceKm, topNeed);
        }).toList();
    }

    private NgoDiscoveryDTO buildDiscoveryDTO(Ngo ngo, double distanceKm, Need topNeed) {
        return new NgoDiscoveryDTO(
                ngo.getId(), ngo.getName(), ngo.getAddress(), distanceKm,
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : "NEW",
                topNeed != null ? topNeed.getItemName() : null,
                topNeed != null ? topNeed.getQuantityRemaining() : 0,
                topNeed != null ? topNeed.getUrgency().name() : null,
                topNeed != null ? topNeed.getCategory().name() : "OTHER",
                ngo.getLat(), ngo.getLng(), ngo.getPhotoUrl()
        );
    }

    private NgoDetailResponse toNgoDetailResponse(Ngo ngo) {
        List<NeedDetailResponse> activeNeeds = needRepository.findByNgoAndStatusIn(
                        ngo, List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED))
                .stream()
                .map(needService::toNeedDetailResponse)
                .toList();

        return new NgoDetailResponse(
                ngo.getId(),
                ngo.getName(),
                ngo.getAddress(),
                ngo.getContactEmail(),
                ngo.getContactPhone(),
                ngo.getDescription(),
                ngo.getCategoryOfWork() != null ? ngo.getCategoryOfWork().name() : null,
                ngo.getPhotoUrl(),
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : null,
                ngo.getVerifiedAt(),
                ngo.getLat(),
                ngo.getLng(),
                activeNeeds
        );
    }

    private boolean checkProfileComplete(Ngo ngo) {
        return ngo.getName() != null && !ngo.getName().isBlank()
                && ngo.getAddress() != null && !ngo.getAddress().isBlank()
                && ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()
                && ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()
                && ngo.getDescription() != null && ngo.getDescription().length() >= 50
                && ngo.getCategoryOfWork() != null;
    }

    private void applyAddressUpdate(Ngo ngo, String newAddress) {
        String trimmedAddress = newAddress.trim();
        if (trimmedAddress.isBlank()) {
            ngo.setAddress(trimmedAddress);
            ngo.setLat(null);
            ngo.setLng(null);
            return;
        }

        GeocodingService.GeocodedPoint point = geocodingService.geocode(trimmedAddress);
        ngo.setAddress(trimmedAddress);
        ngo.setLat(point.lat());
        ngo.setLng(point.lng());
    }
}
