package com.aidonormatcher.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirebaseTokenService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseUserInfo verifyIdToken(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new FirebaseUserInfo(
                    token.getUid(),
                    token.getEmail(),
                    token.isEmailVerified(),
                    token.getName());
        } catch (FirebaseAuthException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid Firebase ID token.");
        }
    }
}
