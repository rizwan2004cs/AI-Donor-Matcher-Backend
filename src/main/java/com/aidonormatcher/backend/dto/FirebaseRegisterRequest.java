package com.aidonormatcher.backend.dto;

import com.aidonormatcher.backend.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FirebaseRegisterRequest(
        @NotBlank String fullName,
        @NotNull Role role,
        String location) {
}
