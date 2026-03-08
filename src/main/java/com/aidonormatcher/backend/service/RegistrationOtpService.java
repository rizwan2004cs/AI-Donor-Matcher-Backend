package com.aidonormatcher.backend.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RegistrationOtpService {

    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;

    @Data
    public static class OtpData {
        private String otp;
        private LocalDateTime expiresAt;
        private int attempts;

        public OtpData(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.attempts = 0;
        }
    }

    public String generateAndStoreOtp(String email) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1_000_000));
        otpStore.put(email, new OtpData(code, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));
        return code;
    }

    public void verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            throw new RuntimeException("No OTP requested for this email.");
        }

        if (otpData.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpStore.remove(email);
            throw new RuntimeException("Invalid or expired code.");
        }

        if (!otpData.getOtp().equals(otp)) {
            otpData.setAttempts(otpData.getAttempts() + 1);
            if (otpData.getAttempts() >= MAX_OTP_ATTEMPTS) {
                otpStore.remove(email);
                throw new RuntimeException("Too many invalid attempts. Please request a new code.");
            }
            throw new RuntimeException("Invalid or expired code.");
        }
    }

    public void clearOtp(String email) {
        otpStore.remove(email);
    }
}
