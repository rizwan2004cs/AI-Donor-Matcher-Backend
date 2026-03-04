package com.aidonormatcher.backend.dto;

import java.time.LocalDateTime;

public record PledgeResponse(
        Long pledgeId,
        Double ngoLat,
        Double ngoLng,
        String ngoAddress,
        String ngoContactEmail,
        LocalDateTime expiresAt
) {}
