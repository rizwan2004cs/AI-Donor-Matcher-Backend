package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.repository.UserRepository;
import com.google.firebase.auth.EmailIdentifier;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GetUsersResult;
import com.google.firebase.auth.ImportUserRecord;
import com.google.firebase.auth.UserImportOptions;
import com.google.firebase.auth.UserImportResult;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.hash.Bcrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirebaseUserMigrationService {

    private static final int LOOKUP_BATCH_SIZE = 100;
    private static final int IMPORT_BATCH_SIZE = 1000;

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    @Transactional
    public MigrationSummary migrateExistingUsers() {
        List<User> users = userRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();

        Map<String, UserRecord> existingFirebaseUsers = fetchExistingFirebaseUsers(users);
        List<User> usersToUpdate = new ArrayList<>();
        List<MigrationCandidate> importCandidates = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        int alreadyLinked = 0;
        int linkedToExistingFirebaseUser = 0;
        int skippedWithoutPassword = 0;

        for (User user : users) {
            if (user.getFirebaseUid() != null && !user.getFirebaseUid().isBlank()) {
                alreadyLinked++;
                continue;
            }

            if (user.getEmail() == null || user.getEmail().isBlank()) {
                errors.add("Skipped DB user " + user.getId() + " because email is blank.");
                continue;
            }

            UserRecord existingFirebaseUser = existingFirebaseUsers.get(normalizeEmail(user.getEmail()));
            if (existingFirebaseUser != null) {
                user.setFirebaseUid(existingFirebaseUser.getUid());
                if (existingFirebaseUser.isEmailVerified()) {
                    user.setEmailVerified(true);
                }
                usersToUpdate.add(user);
                linkedToExistingFirebaseUser++;
                continue;
            }

            if (user.getPassword() == null || user.getPassword().isBlank()) {
                skippedWithoutPassword++;
                errors.add("Skipped " + user.getEmail() + " because no password hash is stored in the database.");
                continue;
            }

            String firebaseUid = buildFirebaseUid(user);
            ImportUserRecord importUserRecord = ImportUserRecord.builder()
                    .setUid(firebaseUid)
                    .setEmail(user.getEmail())
                    .setDisplayName(user.getFullName())
                    .setEmailVerified(user.isEmailVerified())
                    .setPasswordHash(user.getPassword().getBytes(StandardCharsets.UTF_8))
                    .build();

            importCandidates.add(new MigrationCandidate(user, firebaseUid, importUserRecord));
        }

        int imported = 0;
        int failedImports = 0;

        for (List<MigrationCandidate> batch : batches(importCandidates, IMPORT_BATCH_SIZE)) {
            List<ImportUserRecord> records = batch.stream()
                    .map(MigrationCandidate::record)
                    .toList();

            try {
                UserImportResult result = firebaseAuth.importUsers(records, UserImportOptions.withHash(Bcrypt.getInstance()));
                Map<Integer, String> batchErrors = new HashMap<>();
                result.getErrors().forEach(errorInfo -> batchErrors.put(errorInfo.getIndex(), errorInfo.getReason()));

                for (int index = 0; index < batch.size(); index++) {
                    MigrationCandidate candidate = batch.get(index);
                    String failureReason = batchErrors.get(index);
                    if (failureReason != null) {
                        failedImports++;
                        errors.add("Failed to import " + candidate.user().getEmail() + ": " + failureReason);
                        continue;
                    }

                    candidate.user().setFirebaseUid(candidate.firebaseUid());
                    usersToUpdate.add(candidate.user());
                    imported++;
                }
            } catch (FirebaseAuthException ex) {
                failedImports += batch.size();
                for (MigrationCandidate candidate : batch) {
                    errors.add("Failed to import " + candidate.user().getEmail() + ": " + ex.getMessage());
                }
            }
        }

        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
        }

        return new MigrationSummary(
                users.size(),
                alreadyLinked,
                linkedToExistingFirebaseUser,
                imported,
                skippedWithoutPassword,
                failedImports,
                errors
        );
    }

    private Map<String, UserRecord> fetchExistingFirebaseUsers(List<User> users) {
        List<EmailIdentifier> identifiers = users.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(EmailIdentifier::new)
                .toList();

        Map<String, UserRecord> existingUsers = new HashMap<>();
        for (List<EmailIdentifier> batch : batches(identifiers, LOOKUP_BATCH_SIZE)) {
            try {
                GetUsersResult result = firebaseAuth.getUsers(new ArrayList<>(batch));
                for (UserRecord userRecord : result.getUsers()) {
                    if (userRecord.getEmail() != null && !userRecord.getEmail().isBlank()) {
                        existingUsers.put(normalizeEmail(userRecord.getEmail()), userRecord);
                    }
                }
            } catch (FirebaseAuthException ex) {
                throw new RuntimeException("Failed to look up existing Firebase users: " + ex.getMessage(), ex);
            }
        }

        return existingUsers;
    }

    private <T> List<List<T>> batches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int start = 0; start < items.size(); start += batchSize) {
            batches.add(items.subList(start, Math.min(start + batchSize, items.size())));
        }
        return batches;
    }

    private String buildFirebaseUid(User user) {
        return "legacy-user-" + user.getId();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public record MigrationSummary(
            int totalUsers,
            int alreadyLinked,
            int linkedToExistingFirebaseUser,
            int imported,
            int skippedWithoutPassword,
            int failedImports,
            List<String> errors
    ) {
    }

    private record MigrationCandidate(User user, String firebaseUid, ImportUserRecord record) {
    }
}
