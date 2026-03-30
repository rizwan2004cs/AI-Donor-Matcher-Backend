package com.aidonormatcher.backend.dto;

import java.time.LocalDateTime;

public record AdminNgoSummaryResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String address,
        String photoUrl,
        String documentUrl,
        String status,
        String categoryOfWork,
        Integer trustScore,
        String trustTier,
        String rejectionReason,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt
) {
}
