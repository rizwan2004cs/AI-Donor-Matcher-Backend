package com.aidonormatcher.backend.service;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NgoService {

    private final NgoRepository ngoRepository;
    private final NeedRepository needRepository;
    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;

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
        if (req.address() != null) ngo.setAddress(req.address());
        if (req.contactEmail() != null) ngo.setContactEmail(req.contactEmail());
        if (req.contactPhone() != null) ngo.setContactPhone(req.contactPhone());
        if (req.description() != null) ngo.setDescription(req.description());
        if (req.categoryOfWork() != null) ngo.setCategoryOfWork(req.categoryOfWork());

        ngo.setProfileComplete(checkProfileComplete(ngo));

        // Geocode address if provided
        if (req.address() != null) {
            geocodeAddress(ngo);
        }

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

    public Ngo getNgoById(Long id) {
        return ngoRepository.findById(id)
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
            Ngo ngo = (Ngo) row[0];
            double distanceKm = ((Number) row[1]).doubleValue();

            Need topNeed = needRepository
                    .findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(
                            ngo, List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_PLEDGED));

            return buildDiscoveryDTO(ngo, distanceKm, topNeed);
        }).toList();
    }

    private NgoDiscoveryDTO buildDiscoveryDTO(Ngo ngo, double distanceKm, Need topNeed) {
        return new NgoDiscoveryDTO(
                ngo.getId(), ngo.getName(), distanceKm,
                ngo.getTrustScore(),
                ngo.getTrustTier() != null ? ngo.getTrustTier().name() : "NEW",
                topNeed != null ? topNeed.getItemName() : null,
                topNeed != null ? topNeed.getQuantityRemaining() : 0,
                topNeed != null ? topNeed.getUrgency().name() : null,
                topNeed != null ? topNeed.getCategory().name() : "OTHER",
                ngo.getLat(), ngo.getLng(), ngo.getPhotoUrl()
        );
    }

    private boolean checkProfileComplete(Ngo ngo) {
        return ngo.getName() != null && !ngo.getName().isBlank()
                && ngo.getAddress() != null && !ngo.getAddress().isBlank()
                && ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()
                && ngo.getContactPhone() != null && !ngo.getContactPhone().isBlank()
                && ngo.getDescription() != null && ngo.getDescription().length() >= 50
                && ngo.getCategoryOfWork() != null
                && ngo.getLat() != null
                && ngo.getLng() != null
                && ngo.getPhotoUrl() != null && !ngo.getPhotoUrl().isBlank()
                && ngo.getDocumentUrl() != null && !ngo.getDocumentUrl().isBlank();
    }

    @SuppressWarnings("unchecked")
    private void geocodeAddress(Ngo ngo) {
        try {
            String encoded = URLEncoder.encode(ngo.getAddress(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AIDonorMatcher/1.0");
            ResponseEntity<List> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
            if (res.getBody() != null && !res.getBody().isEmpty()) {
                Map<?, ?> result = (Map<?, ?>) res.getBody().get(0);
                ngo.setLat(Double.parseDouble(result.get("lat").toString()));
                ngo.setLng(Double.parseDouble(result.get("lon").toString()));
            }
        } catch (Exception e) {
            // Geocoding failure is non-fatal; log and continue
        }
    }
}
