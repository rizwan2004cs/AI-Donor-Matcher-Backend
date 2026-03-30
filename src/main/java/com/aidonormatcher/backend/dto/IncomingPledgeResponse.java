package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Incoming pledge summary for NGO dashboard notifications.")
public record IncomingPledgeResponse(
        @Schema(description = "Pledge id.", example = "44")
        Long pledgeId,
        @Schema(description = "Need id receiving the pledge.", example = "12")
        Long needId,
        @Schema(description = "Donor display name.", example = "Alice Donor")
        String donorName,
        @Schema(description = "Donor email address.", example = "alice@example.com")
        String donorEmail,
        @Schema(description = "Item pledged.", example = "Rice packs")
        String itemName,
        @Schema(description = "Need category.", example = "FOOD")
        String category,
        @Schema(description = "Pledged quantity.", example = "10")
        int quantity,
        @Schema(description = "Pledge status.", example = "ACTIVE")
        String status,
        @Schema(description = "Pledge creation timestamp.", example = "2026-03-30T10:15:00")
        LocalDateTime createdAt,
        @Schema(description = "Pledge expiry timestamp when still active.", example = "2026-04-01T10:15:00")
        LocalDateTime expiresAt,
        @Schema(description = "Need total required quantity.", example = "22")
        int needQuantityRequired,
        @Schema(description = "Need total committed quantity from all non-cancelled pledges.", example = "15")
        int needQuantityPledged,
        @Schema(description = "Need quantity confirmed as received by the NGO.", example = "15")
        int needQuantityReceived,
        @Schema(description = "Need quantity still outstanding based on received deliveries.", example = "7")
        int needQuantityRemaining) {
}
