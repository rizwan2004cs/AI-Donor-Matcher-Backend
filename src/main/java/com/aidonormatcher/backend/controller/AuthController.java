package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.EmailRequest;
import com.aidonormatcher.backend.dto.FirebaseRegisterRequest;
import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.MessageResponse;
import com.aidonormatcher.backend.dto.OtpVerificationRequest;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.service.AuthService;
import com.aidonormatcher.backend.service.FirebaseAuthService;
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
    private final FirebaseAuthService firebaseAuthService;

    @Operation(
            summary = "Register a donor or NGO account using OTP",
            description = "Creates the account only after the OTP submitted with the registration payload is validated, then returns the authenticated user payload for immediate auto-login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account registered successfully and logged in"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid OTP")
    })
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> registerJson(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request, null);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Register an NGO account with multipart form data",
            description = "Accepts the registration fields and an optional NGO document file in the same request, then returns the authenticated user payload for immediate auto-login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account registered successfully and logged in"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid OTP")
    })
    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LoginResponse> registerMultipart(
            @Valid @ModelAttribute RegisterRequest request,
            @Parameter(description = "Optional NGO verification document")
            @RequestPart(value = "documents", required = false) org.springframework.web.multipart.MultipartFile document) {
        LoginResponse response = authService.register(request, document);
        return ResponseEntity.ok(response);
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

    @Operation(summary = "Complete Firebase-backed registration and return the authenticated app session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firebase user linked to the application successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or Firebase token issue")
    })
    @PostMapping(value = "/firebase/register", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> firebaseRegisterJson(
            @Valid @RequestBody FirebaseRegisterRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        LoginResponse response = firebaseAuthService.register(request, null, extractBearerToken(authorizationHeader));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Complete Firebase-backed NGO registration with multipart data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firebase NGO user linked to the application successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or Firebase token issue")
    })
    @PostMapping(value = "/firebase/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LoginResponse> firebaseRegisterMultipart(
            @Valid @ModelAttribute FirebaseRegisterRequest request,
            @RequestPart(value = "documents", required = false) org.springframework.web.multipart.MultipartFile document,
            @RequestHeader("Authorization") String authorizationHeader) {
        LoginResponse response = firebaseAuthService.register(request, document, extractBearerToken(authorizationHeader));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Log in with a Firebase ID token and return the app session payload")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Firebase login successful"),
            @ApiResponse(responseCode = "400", description = "Firebase token invalid or app account not linked")
    })
    @PostMapping("/firebase/login")
    public ResponseEntity<LoginResponse> firebaseLogin(@RequestHeader("Authorization") String authorizationHeader) {
        LoginResponse response = firebaseAuthService.login(extractBearerToken(authorizationHeader));
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

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Firebase bearer token.");
        }
        return authorizationHeader.substring(7);
    }
}
