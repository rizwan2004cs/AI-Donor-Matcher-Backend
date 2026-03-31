package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.dto.FirebaseRegisterRequest;
import com.aidonormatcher.backend.dto.LoginResponse;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FirebaseAuthService {

    private final FirebaseTokenService firebaseTokenService;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    public LoginResponse login(String idToken) {
        User user = resolveAuthenticatedUser(idToken)
                .orElseThrow(() -> new RuntimeException("No application account found for this Firebase user. Complete registration first."));
        return buildLoginResponse(user, idToken);
    }

    @Transactional
    public LoginResponse register(FirebaseRegisterRequest request, MultipartFile document, String idToken) {
        FirebaseUserInfo firebaseUser = firebaseTokenService.verifyIdToken(idToken);
        String email = requireEmail(firebaseUser);

        Optional<User> linkedUser = userRepository.findByFirebaseUid(firebaseUser.uid());
        Optional<User> emailUser = userRepository.findByEmail(email);

        User user;
        boolean createdUser = false;
        if (linkedUser.isPresent()) {
            user = linkedUser.get();
            if (user.getRole() != request.role()) {
                throw new RuntimeException("This Firebase account is already linked to a " + user.getRole().name() + " account.");
            }
        } else if (emailUser.isPresent()) {
            user = emailUser.get();
            if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(firebaseUser.uid())) {
                throw new RuntimeException("Email already registered with a different Firebase account.");
            }
            if (user.getRole() != request.role()) {
                throw new RuntimeException("Email already registered with role " + user.getRole().name() + ".");
            }
        } else {
            user = User.builder()
                    .email(email)
                    .role(request.role())
                    .createdAt(LocalDateTime.now())
                    .build();
            createdUser = true;
        }

        user.setFirebaseUid(firebaseUser.uid());
        user.setFullName(request.fullName());
        user.setLocation(request.location());
        user.setEmailVerified(firebaseUser.emailVerified());
        user = userRepository.save(user);

        if (request.role() == Role.NGO) {
            syncNgoProfile(user, document, createdUser);
        }

        return buildLoginResponse(user, idToken);
    }

    @Transactional
    public Optional<User> resolveAuthenticatedUser(String idToken) {
        FirebaseUserInfo firebaseUser = firebaseTokenService.verifyIdToken(idToken);
        String email = firebaseUser.email();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        Optional<User> linkedUser = userRepository.findByFirebaseUid(firebaseUser.uid());
        if (linkedUser.isPresent()) {
            return Optional.of(syncLinkedUser(linkedUser.get(), firebaseUser, false));
        }

        Optional<User> emailUser = userRepository.findByEmail(email);
        if (emailUser.isEmpty()) {
            return Optional.empty();
        }

        User user = emailUser.get();
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().equals(firebaseUser.uid())) {
            return Optional.empty();
        }

        return Optional.of(syncLinkedUser(user, firebaseUser, true));
    }

    private LoginResponse buildLoginResponse(User user, String token) {
        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name());
    }

    private String requireEmail(FirebaseUserInfo firebaseUser) {
        if (firebaseUser.email() == null || firebaseUser.email().isBlank()) {
            throw new RuntimeException("Firebase account does not include an email address.");
        }
        return firebaseUser.email();
    }

    private User syncLinkedUser(User user, FirebaseUserInfo firebaseUser, boolean linkFirebaseUid) {
        boolean changed = false;
        if (linkFirebaseUid && (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank())) {
            user.setFirebaseUid(firebaseUser.uid());
            changed = true;
        }
        if (firebaseUser.emailVerified() && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            changed = true;
        }
        if (changed) {
            return userRepository.save(user);
        }
        return user;
    }

    private void syncNgoProfile(User user, MultipartFile document, boolean notifyAdmins) {
        Ngo ngo = ngoRepository.findByUser(user)
                .orElseGet(() -> Ngo.builder()
                        .user(user)
                        .status(NgoStatus.PENDING)
                        .profileComplete(false)
                        .trustScore(0)
                        .trustTier(TrustTier.NEW)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (document != null && !document.isEmpty()) {
            try {
                ngo.setDocumentUrl(cloudinaryService.uploadDocument(document));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to upload document: " + ex.getMessage());
            }
        }

        boolean isNewNgo = ngo.getId() == null;
        ngoRepository.save(ngo);

        if (notifyAdmins && isNewNgo) {
            for (User admin : userRepository.findByRole(Role.ADMIN)) {
                emailService.sendNgoApplicationAlert(admin, ngo);
            }
        }
    }
}
