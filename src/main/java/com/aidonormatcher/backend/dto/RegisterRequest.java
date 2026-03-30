package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Registration payload used by donors and NGOs after OTP verification has started.")
public record RegisterRequest(
                @Schema(description = "Full name of the donor or NGO account owner.", example = "Helping Hands Foundation")
                @NotBlank String fullName,
                @Schema(description = "Email address for login and OTP delivery.", example = "ngo@example.com")
                @NotBlank @Email String email,
                @Schema(description = "Account password.", example = "StrongPassword123")
                @NotBlank String password,
                @Schema(description = "Role for the new account.", example = "NGO")
                @NotNull Role role,
                @Schema(description = "Optional location text shown on the profile.", example = "Colombo")
                String location,
                @Schema(description = "OTP code previously sent to the email address.", example = "123456")
                @NotBlank String otp) {
}
