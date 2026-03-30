package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public NGO summary returned in discovery results.")
public record NgoDiscoveryDTO(
        @Schema(description = "NGO id.", example = "5")
        Long id,
        @Schema(description = "NGO display name.", example = "Helping Hands")
        String name,
        @Schema(description = "Public NGO address.", example = "Nellore, Andhra Pradesh, India")
        String address,
        @Schema(description = "Distance from the user's supplied coordinates in kilometers.", example = "4.2")
        double distanceKm,
        @Schema(description = "Calculated trust score.", example = "82")
        int trustScore,
        @Schema(description = "Human-friendly trust tier.", example = "Trusted")
        String trustTier,
        @Schema(description = "Name of the top unmet need.", example = "Rice packs")
        String topNeedItem,
        @Schema(description = "Remaining quantity for the top need.", example = "20")
        int topNeedQuantityRemaining,
        @Schema(description = "Urgency for the top need.", example = "HIGH")
        String topNeedUrgency,
        @Schema(description = "Category for the top need.", example = "FOOD")
        String topNeedCategory,
        @Schema(description = "NGO latitude.", example = "6.9271")
        Double lat,
        @Schema(description = "NGO longitude.", example = "79.8612")
        Double lng,
        @Schema(description = "Public NGO photo URL.", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
        String photoUrl
) {}
