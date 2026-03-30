package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response returned after a pledge is created.")
public record PledgeResponse(
        @Schema(description = "Created pledge id.", example = "44")
        Long pledgeId,
        @Schema(description = "Latitude of the NGO destination.", example = "6.9271")
        Double ngoLat,
        @Schema(description = "Longitude of the NGO destination.", example = "79.8612")
        Double ngoLng,
        @Schema(description = "Address of the NGO destination.", example = "123 Main Street, Colombo")
        String ngoAddress,
        @Schema(description = "NGO contact email for coordination.", example = "contact@helpinghands.org")
        String ngoContactEmail,
        @Schema(description = "Time when the pledge expires if not completed.", example = "2026-03-31T10:30:00")
        LocalDateTime expiresAt
) {}
