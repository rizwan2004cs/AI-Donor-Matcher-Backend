package com.aidonormatcher.backend.dto;

public record RegisterLoginResponse(
        String message,
        String token,
        LoginResponse.UserInfo user) {
}

