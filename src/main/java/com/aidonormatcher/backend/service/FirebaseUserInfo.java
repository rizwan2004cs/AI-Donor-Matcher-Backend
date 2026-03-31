package com.aidonormatcher.backend.service;

public record FirebaseUserInfo(
        String uid,
        String email,
        boolean emailVerified,
        String displayName) {
}
