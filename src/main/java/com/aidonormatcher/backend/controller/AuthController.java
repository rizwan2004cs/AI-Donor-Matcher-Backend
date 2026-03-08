package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.RegisterLoginResponse;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String REGISTRATION_MESSAGE = "Registration successful. Please check your email to verify your account.";

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterLoginResponse> registerJson(@Valid @RequestBody RegisterRequest request) {
        LoginResponse loginResponse = authService.register(request, null);
        RegisterLoginResponse body = new RegisterLoginResponse(
                REGISTRATION_MESSAGE,
                loginResponse.token(),
                loginResponse.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RegisterLoginResponse> registerMultipart(
            @Valid @ModelAttribute RegisterRequest request,
            @RequestPart(value = "documents", required = false) org.springframework.web.multipart.MultipartFile document) {
        LoginResponse loginResponse = authService.register(request, document);
        RegisterLoginResponse body = new RegisterLoginResponse(
                REGISTRATION_MESSAGE,
                loginResponse.token(),
                loginResponse.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }
        authService.resendVerification(email);
        return ResponseEntity.ok(Map.of("message", "Verification email resent. Please check your inbox."));
    }

    @PostMapping("/send-registration-otp")
    public ResponseEntity<Map<String, String>> sendRegistrationOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }
        authService.sendRegistrationOtp(email);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to your email"));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }
        authService.sendOtp(email);
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email and OTP are required."));
        }
        authService.verifyOtp(email, otp);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }
}
