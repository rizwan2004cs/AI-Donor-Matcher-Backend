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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already registered.");
        }

        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .emailVerified(false)
                .location(req.location())
                .createdAt(LocalDateTime.now())
                .build();

        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        userRepository.save(user);

        // If NGO, create linked Ngo entity
        if (req.role() == Role.NGO) {
            Ngo ngo = Ngo.builder()
                    .user(user)
                    .status(NgoStatus.PENDING)
                    .profileComplete(false)
                    .trustScore(0)
                    .trustTier(TrustTier.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();
            ngoRepository.save(ngo);
        }

        emailService.sendVerificationEmail(user, token);
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

        String jwt = jwtService.generateToken(user);

        return new LoginResponse(
                jwt,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified()
        );
    }
}
