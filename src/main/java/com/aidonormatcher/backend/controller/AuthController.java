package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.EmailRequest;
import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.MessageResponse;
import com.aidonormatcher.backend.dto.OtpVerificationRequest;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, OTP verification, and login endpoints.")
public class AuthController {

    private final AuthService authService;

    private static final String REGISTRATION_MESSAGE = "Registration successful. You can now log in.";

    @Operation(
            summary = "Register a donor or NGO account using OTP",
            description = "Creates the account only after the OTP submitted with the registration payload is validated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid OTP")
    })
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> registerJson(@Valid @RequestBody RegisterRequest request) {
        authService.register(request, null);
        return ResponseEntity.ok(new MessageResponse(REGISTRATION_MESSAGE));
    }

    @Operation(
            summary = "Register an NGO account with multipart form data",
            description = "Accepts the registration fields and an optional NGO document file in the same request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid OTP")
    })
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> registerMultipart(
            @Valid @ModelAttribute RegisterRequest request,
            @Parameter(description = "Optional NGO verification document")
            @RequestPart(value = "documents", required = false) org.springframework.web.multipart.MultipartFile document) {
        authService.register(request, document);
        return ResponseEntity.ok(new MessageResponse(REGISTRATION_MESSAGE));
    }

    @Operation(
            summary = "Legacy link verification endpoint",
            description = "Kept for compatibility, but the active registration flow uses OTP verification instead."
    )
    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Parameter(description = "Legacy verification token") @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Email verified."));
    }

    @Operation(summary = "Authenticate a user and return a JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resend verification OTP")
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code sent to your email."));
    }

    @Operation(summary = "Send registration OTP")
    @PostMapping("/send-registration-otp")
    public ResponseEntity<MessageResponse> sendRegistrationOtp(@Valid @RequestBody EmailRequest request) {
        authService.sendRegistrationOtp(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code sent to your email"));
    }

    @Operation(summary = "Send OTP to an existing user email")
    @PostMapping("/send-otp")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody EmailRequest request) {
        authService.sendOtp(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code sent"));
    }

    @Operation(summary = "Verify email using OTP")
    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        authService.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(new MessageResponse("Email verified"));
    }
}
