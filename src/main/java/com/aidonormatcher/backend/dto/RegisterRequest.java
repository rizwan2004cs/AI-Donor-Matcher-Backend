package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
                @NotBlank String fullName,
                @NotBlank @Email String email,
                @NotBlank String password,
                @NotNull Role role,
                String location,
                @NotBlank String otp) {
}
