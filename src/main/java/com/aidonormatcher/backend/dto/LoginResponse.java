package com.aidonormatcher.backend.dto;

public record LoginResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        String role,
        boolean emailVerified
) {}
