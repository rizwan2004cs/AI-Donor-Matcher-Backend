package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.service.AuthService;
import com.aidonormatcher.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void registerJson_validRequest_returnsCreatedResponse() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Alice Donor",
                "alice@example.com",
                "password123",
                Role.DONOR,
                "Colombo",
                "123456");

        LoginResponse response = new LoginResponse(
                "jwt-token",
                new LoginResponse.UserInfo(1L, "Alice Donor", "alice@example.com", "DONOR", true, true));

        when(authService.register(any(RegisterRequest.class), eq(null))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.role").value("DONOR"))
                .andExpect(jsonPath("$.user.profileComplete").value(true));

        verify(authService).register(any(RegisterRequest.class), eq(null));
    }

    @Test
    void registerMultipart_validRequest_returnsCreatedResponse() throws Exception {
        LoginResponse response = new LoginResponse(
                "ngo-jwt",
                new LoginResponse.UserInfo(2L, "Helping Hands", "ngo@example.com", "NGO", true, false));

        when(authService.register(any(RegisterRequest.class), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/auth/register")
                        .file("documents", "sample".getBytes())
                        .param("fullName", "Helping Hands")
                        .param("email", "ngo@example.com")
                        .param("password", "password123")
                        .param("role", "NGO")
                        .param("otp", "654321")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("ngo-jwt"))
                .andExpect(jsonPath("$.user.role").value("NGO"))
                .andExpect(jsonPath("$.user.profileComplete").value(false));
    }

    @Test
    void registerJson_invalidRequest_returnsBadRequest() throws Exception {
        Map<String, Object> invalidRequest = Map.of(
                "fullName", "",
                "email", "not-an-email",
                "password", "",
                "role", "DONOR",
                "otp", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void verifyEmail_validToken_returnsSuccessMessage() throws Exception {
        doNothing().when(authService).verifyEmail("valid-token");

        mockMvc.perform(get("/api/auth/verify").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. You can now log in."));
    }

    @Test
    void verifyEmail_invalidToken_returnsBadRequest() throws Exception {
        doThrow(new RuntimeException("Invalid verification token."))
                .when(authService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/auth/verify").param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid verification token."));
    }

    @Test
    void login_validCredentials_returnsLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        LoginResponse response = new LoginResponse(
                "jwt-token",
                new LoginResponse.UserInfo(1L, "Alice Donor", "alice@example.com", "DONOR", true, true));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.role").value("DONOR"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    void resendVerification_missingEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required."));
    }

    @Test
    void resendVerification_validEmail_returnsSuccessMessage() throws Exception {
        doNothing().when(authService).resendVerification("alice@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "alice@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email resent. Please check your inbox."));
    }
}
