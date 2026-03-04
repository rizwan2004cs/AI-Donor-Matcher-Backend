package com.aidonormatcher.backend.dto;

public record NgoDiscoveryDTO(
        Long id,
        String name,
        double distanceKm,
        int trustScore,
        String trustTier,
        String topNeedItem,
        int topNeedQuantityRemaining,
        String topNeedUrgency,
        String topNeedCategory,
        Double lat,
        Double lng,
        String photoUrl
) {}
