package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request payload.")
public record LoginRequest(
        @Schema(description = "User email address.", example = "alice@example.com")
        @NotBlank @Email String email,
        @Schema(description = "Plain text password.", example = "password123")
        @NotBlank String password
) {}
