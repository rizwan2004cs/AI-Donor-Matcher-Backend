package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic API response containing a message.")
public record MessageResponse(
        @Schema(description = "Human-readable API response message.", example = "Registration successful. You can now log in.")
        String message) {
}
