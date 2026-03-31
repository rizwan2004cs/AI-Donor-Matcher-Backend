package com.aidonormatcher.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class FirebaseConfig {

    private static final Path LOCAL_ENV_PATH = Path.of(".env");
    private static final Map<String, String> LOCAL_ENV_VALUES = new ConcurrentHashMap<>();

    @Bean
    public FirebaseApp firebaseApp(
            @Value("${firebase.admin.credentials-path}") String credentialsPath,
            @Value("${firebase.admin.credentials-json}") String credentialsJson,
            @Value("${firebase.admin.project-id}") String projectId,
            @Value("${firebase.admin.client-email}") String clientEmail,
            @Value("${firebase.admin.private-key}") String privateKey) throws IOException {
        credentialsPath = resolveConfigValue(credentialsPath, "FIREBASE_ADMIN_CREDENTIALS_PATH");
        credentialsJson = resolveConfigValue(credentialsJson, "FIREBASE_ADMIN_CREDENTIALS_JSON");
        projectId = resolveConfigValue(projectId, "FIREBASE_ADMIN_PROJECT_ID");
        clientEmail = resolveConfigValue(clientEmail, "FIREBASE_ADMIN_CLIENT_EMAIL");
        privateKey = resolveConfigValue(privateKey, "FIREBASE_ADMIN_PRIVATE_KEY");

        GoogleCredentials credentials;
        String resolvedProjectId = projectId;
        if (!credentialsJson.isBlank()) {
            resolvedProjectId = firstNonBlank(projectId, extractProjectId(credentialsJson));
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(
                    credentialsJson.replace("\\n", "\n").getBytes(StandardCharsets.UTF_8)));
        } else if (!credentialsPath.isBlank()) {
            Path credentialsFile = Path.of(credentialsPath);
            if (!Files.exists(credentialsFile)) {
                throw new IllegalStateException("Firebase Admin credentials file not found: " + credentialsFile);
            }
            resolvedProjectId = firstNonBlank(projectId, extractProjectId(Files.readString(credentialsFile)));
            credentials = GoogleCredentials.fromStream(Files.newInputStream(credentialsFile));
        } else {
            if (projectId.isBlank() || clientEmail.isBlank() || privateKey.isBlank()) {
                throw new IllegalStateException("Firebase Admin credentials are missing. Set FIREBASE_ADMIN_CREDENTIALS_JSON, FIREBASE_ADMIN_CREDENTIALS_PATH, or FIREBASE_ADMIN_PROJECT_ID, FIREBASE_ADMIN_CLIENT_EMAIL, and FIREBASE_ADMIN_PRIVATE_KEY.");
            }

            String normalizedPrivateKey = privateKey.replace("\\n", "\n");
            Map<String, String> credentialsMap = new HashMap<>();
            credentialsMap.put("type", "service_account");
            credentialsMap.put("project_id", projectId);
            credentialsMap.put("client_email", clientEmail);
            credentialsMap.put("private_key", normalizedPrivateKey);

            byte[] credentialsBytes = new ObjectMapper().writeValueAsBytes(credentialsMap);
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsBytes));
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(credentials);
        if (!resolvedProjectId.isBlank()) {
            optionsBuilder.setProjectId(resolvedProjectId);
        }
        FirebaseOptions options = optionsBuilder.build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }

        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private String resolveConfigValue(String currentValue, String envKey) throws IOException {
        String systemValue = System.getenv(envKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return trimWrappingQuotes(systemValue);
        }

        loadLocalEnvIfPresent();
        String localValue = LOCAL_ENV_VALUES.get(envKey);
        if (localValue != null && !localValue.isBlank()) {
            return localValue;
        }

        if (currentValue != null && !currentValue.isBlank()) {
            return trimWrappingQuotes(currentValue);
        }

        return "";
    }

    private void loadLocalEnvIfPresent() throws IOException {
        if (!LOCAL_ENV_VALUES.isEmpty() || !Files.exists(LOCAL_ENV_PATH)) {
            return;
        }

        List<String> lines = Files.readAllLines(LOCAL_ENV_PATH);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            int separatorIndex = line.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            LOCAL_ENV_VALUES.put(key, trimWrappingQuotes(value));
        }
    }

    private String trimWrappingQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value == null ? "" : value;
        }

        boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
        boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
        if (doubleQuoted || singleQuoted) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String extractProjectId(String credentialsJson) {
        try {
            Map<?, ?> credentialsMap = new ObjectMapper().readValue(credentialsJson, Map.class);
            Object projectId = credentialsMap.get("project_id");
            return projectId instanceof String projectIdValue ? projectIdValue : "";
        } catch (IOException e) {
            return "";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
