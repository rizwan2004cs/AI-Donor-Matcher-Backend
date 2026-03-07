package com.aidonormatcher.backend.dto;

public record LoginResponse(
                String token,
                UserInfo user) {
        public record UserInfo(
                        Long userId,
                        String fullName,
                        String email,
                        String role,
                        boolean emailVerified) {
        }
}
