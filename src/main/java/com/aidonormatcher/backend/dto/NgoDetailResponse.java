package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Public NGO detail response used by the NGO profile page.")
public record NgoDetailResponse(
        @Schema(description = "NGO id.", example = "5")
        Long id,
        @Schema(description = "NGO display name.", example = "Helping Hands")
        String name,
        @Schema(description = "Public NGO address.", example = "123 Main Street, Colombo")
        String address,
        @Schema(description = "Public NGO contact email.", example = "contact@helpinghands.org")
        String contactEmail,
        @Schema(description = "Public NGO contact phone.", example = "+94 77 123 4567")
        String contactPhone,
        @Schema(description = "Public NGO description.", example = "Supporting families with essential supplies.")
        String description,
        @Schema(description = "Primary category of work.", example = "FOOD")
        String categoryOfWork,
        @Schema(description = "Public NGO photo URL.", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
        String photoUrl,
        @Schema(description = "Calculated trust score.", example = "82")
        Integer trustScore,
        @Schema(description = "Human-friendly trust tier.", example = "ESTABLISHED")
        String trustTier,
        @Schema(description = "Approval timestamp.", example = "2026-03-30T10:15:00")
        LocalDateTime verifiedAt,
        @Schema(description = "NGO latitude.", example = "6.9271")
        Double lat,
        @Schema(description = "NGO longitude.", example = "79.8612")
        Double lng,
        @Schema(description = "Active needs for this NGO.")
        List<NeedDetailResponse> activeNeeds
) {
}
