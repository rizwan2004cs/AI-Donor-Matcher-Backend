package com.aidonormatcher.backend.dto;

import java.time.LocalDateTime;

public record AdminReportSummaryResponse(
        Long id,
        Long ngoId,
        String ngoName,
        String ngoStatus,
        String reason,
        String reporterEmail,
        LocalDateTime reportedAt
) {
}
