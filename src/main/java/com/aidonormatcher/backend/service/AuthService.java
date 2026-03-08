package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.LoginRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.dto.RegisterRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final RegistrationOtpService registrationOtpService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private String generateOtp() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(code);
    }

    private void startOtpForUser(User user) {
        String otp = generateOtp();
        user.setEmailVerificationOtp(otp);
        user.setEmailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setEmailVerificationOtpAttempts(0);
        userRepository.save(user);
        emailService.sendVerificationOtpEmail(user, otp);
    }

    private LoginResponse buildLoginResponse(User user) {
        String jwt = jwtService.generateToken(user);

        Boolean profileComplete;
        if (user.getRole() == Role.NGO) {
            Ngo ngo = ngoRepository.findByUserId(user.getId())
                    .orElse(null);
            profileComplete = ngo != null && ngo.isProfileComplete();
        } else {
            // Donors are always treated as profile-complete for navigation.
            profileComplete = Boolean.TRUE;
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified(),
                profileComplete);

        return new LoginResponse(jwt, userInfo);
    }

    public void sendRegistrationOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered.");
        }
        String otp = registrationOtpService.generateAndStoreOtp(email);
        emailService.sendPreRegistrationOtpEmail(email, otp);
    }

    @Transactional
    public LoginResponse register(RegisterRequest req, MultipartFile document) {
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already registered.");
        }

        registrationOtpService.verifyOtp(req.email(), req.otp());

        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .emailVerified(true)
                .location(req.location())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // If NGO, create linked Ngo entity and Handle document upload
        if (req.role() == Role.NGO) {
            String docUrl = null;
            if (document != null && !document.isEmpty()) {
                try {
                    docUrl = cloudinaryService.uploadDocument(document);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload document: " + e.getMessage());
                }
            }

            Ngo ngo = Ngo.builder()
                    .user(user)
                    .status(NgoStatus.PENDING)
                    .profileComplete(false)
                    .trustScore(0)
                    .trustTier(TrustTier.NEW)
                    .documentUrl(docUrl)
                    .createdAt(LocalDateTime.now())
                    .build();
            ngoRepository.save(ngo);
        }

        registrationOtpService.clearOtp(req.email());
        return buildLoginResponse(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token."));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        return buildLoginResponse(user);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email."));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        startOtpForUser(user);
    }

    @Transactional
    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email."));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        startOtpForUser(user);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email."));

        if (user.isEmailVerified()) {
            return;
        }

        String storedOtp = user.getEmailVerificationOtp();
        LocalDateTime expiresAt = user.getEmailVerificationOtpExpiresAt();

        boolean expired = expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
        boolean mismatch = storedOtp == null || !storedOtp.equals(otp);

        if (expired || mismatch) {
            Integer attempts = user.getEmailVerificationOtpAttempts();
            int currentAttempts = attempts != null ? attempts : 0;
            currentAttempts++;
            user.setEmailVerificationOtpAttempts(currentAttempts);
            userRepository.save(user);

            if (currentAttempts >= MAX_OTP_ATTEMPTS) {
                throw new RuntimeException("Too many invalid attempts. Please request a new code.");
            }

            throw new RuntimeException("Invalid or expired code.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationOtp(null);
        user.setEmailVerificationOtpExpiresAt(null);
        user.setEmailVerificationOtpAttempts(0);
        userRepository.save(user);
    }
}
