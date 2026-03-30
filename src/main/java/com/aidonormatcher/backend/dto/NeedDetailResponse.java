package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Safe need detail response used for pledge-screen and admin review flows.")
public record NeedDetailResponse(
        @Schema(description = "Need id.", example = "12")
        Long id,
        @Schema(description = "Owning NGO id.", example = "5")
        Long ngoId,
        @Schema(description = "Owning NGO display name.", example = "Helping Hands")
        String ngoName,
        @Schema(description = "Owning NGO address.", example = "123 Main Street, Colombo")
        String ngoAddress,
        @Schema(description = "Owning NGO photo URL.", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
        String ngoPhotoUrl,
        @Schema(description = "Owning NGO trust score.", example = "82")
        Integer ngoTrustScore,
        @Schema(description = "Owning NGO trust tier.", example = "TRUSTED")
        String ngoTrustTier,
        @Schema(description = "Need category.", example = "FOOD")
        String category,
        @Schema(description = "Need item name.", example = "Rice packs")
        String itemName,
        @Schema(description = "Need description.", example = "5kg rice packs for 50 families")
        String description,
        @Schema(description = "Required quantity.", example = "50")
        int quantityRequired,
        @Schema(description = "Currently pledged quantity.", example = "30")
        int quantityPledged,
        @Schema(description = "Remaining quantity still needed.", example = "20")
        int quantityRemaining,
        @Schema(description = "Urgency level.", example = "HIGH")
        String urgency,
        @Schema(description = "Optional expiry date.", example = "2026-04-15")
        LocalDate expiryDate,
        @Schema(description = "Need status.", example = "PARTIALLY_PLEDGED")
        String status,
        @Schema(description = "Creation timestamp.", example = "2026-03-30T10:15:00")
        LocalDateTime createdAt) {
}
