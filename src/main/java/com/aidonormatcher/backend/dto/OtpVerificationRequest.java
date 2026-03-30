package com.aidonormatcher.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body used to verify an OTP code.")
public record OtpVerificationRequest(
        @Schema(description = "User email address.", example = "user@example.com")
        @NotBlank @Email String email,
        @Schema(description = "6-digit OTP code.", example = "123456")
        @NotBlank String otp) {
}
