package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for reporting an NGO.")
public record ReportRequest(
        @Schema(description = "Reason for the report.", example = "Suspicious activity")
        String reason
) {}
