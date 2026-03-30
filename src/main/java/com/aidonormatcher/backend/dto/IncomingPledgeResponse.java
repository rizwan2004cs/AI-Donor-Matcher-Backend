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
        @Schema(description = "Pledged quantity.", example = "10")
        int quantity,
        @Schema(description = "Pledge status.", example = "ACTIVE")
        String status,
        @Schema(description = "Pledge creation timestamp.", example = "2026-03-30T10:15:00")
        LocalDateTime createdAt) {
}
