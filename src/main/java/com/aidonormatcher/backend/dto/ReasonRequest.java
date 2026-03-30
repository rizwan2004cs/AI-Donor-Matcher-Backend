package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body containing a moderation reason.")
public record ReasonRequest(
        @Schema(description = "Reason for the admin action.", example = "Missing supporting documents")
        @NotBlank String reason) {
}
