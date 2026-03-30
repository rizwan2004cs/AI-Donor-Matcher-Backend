package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Safe pledge detail response used for delivery-view and refresh-safe pledge loading.")
public record PledgeDetailResponse(
        @Schema(description = "Pledge id.", example = "44")
        Long pledgeId,
        @Schema(description = "Need id linked to this pledge.", example = "12")
        Long needId,
        @Schema(description = "NGO id linked to this pledge.", example = "5")
        Long ngoId,
        @Schema(description = "NGO display name.", example = "Helping Hands")
        String ngoName,
        @Schema(description = "NGO photo URL.", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
        String ngoPhotoUrl,
        @Schema(description = "Pledged item name.", example = "Rice packs")
        String itemName,
        @Schema(description = "Need category.", example = "FOOD")
        String category,
        @Schema(description = "Pledged quantity.", example = "10")
        int quantity,
        @Schema(description = "Pledge status.", example = "ACTIVE")
        String status,
        @Schema(description = "Pledge creation time.", example = "2026-03-30T10:15:00")
        LocalDateTime createdAt,
        @Schema(description = "Pledge expiry time.", example = "2026-04-01T10:15:00")
        LocalDateTime expiresAt,
        @Schema(description = "NGO latitude.", example = "6.9271")
        Double ngoLat,
        @Schema(description = "NGO longitude.", example = "79.8612")
        Double ngoLng,
        @Schema(description = "NGO address.", example = "123 Main Street, Colombo")
        String ngoAddress,
        @Schema(description = "NGO contact email.", example = "contact@helpinghands.org")
        String ngoContactEmail) {
}
