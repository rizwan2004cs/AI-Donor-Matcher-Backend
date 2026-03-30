package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body containing an email address.")
public record EmailRequest(
        @Schema(description = "User email address.", example = "user@example.com")
        @NotBlank @Email String email) {
}
