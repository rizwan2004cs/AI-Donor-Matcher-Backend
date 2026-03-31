package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response returned after a successful login or OTP-backed registration.")
public record LoginResponse(
        @Schema(description = "JWT bearer token.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "Authenticated user id.", example = "1")
        Long userId,
        @Schema(description = "Full name of the authenticated user.", example = "Alice Donor")
        String fullName,
        @Schema(description = "Authenticated email address.", example = "alice@example.com")
        String email,
        @Schema(description = "Role of the authenticated user.", example = "DONOR")
        String role) {
}
