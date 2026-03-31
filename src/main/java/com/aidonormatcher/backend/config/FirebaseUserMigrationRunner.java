package com.aidonormatcher.backend.config;

import com.aidonormatcher.backend.service.FirebaseUserMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.migration.enabled", havingValue = "true")
public class FirebaseUserMigrationRunner implements ApplicationRunner {

    private final FirebaseUserMigrationService firebaseUserMigrationService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${firebase.migration.exit-after-run:true}")
    private boolean exitAfterRun;

    @Override
    public void run(ApplicationArguments args) {
        FirebaseUserMigrationService.MigrationSummary summary = firebaseUserMigrationService.migrateExistingUsers();

        System.out.println("Firebase user migration completed.");
        System.out.println("Total DB users: " + summary.totalUsers());
        System.out.println("Already linked in DB: " + summary.alreadyLinked());
        System.out.println("Linked to existing Firebase users: " + summary.linkedToExistingFirebaseUser());
        System.out.println("Imported into Firebase: " + summary.imported());
        System.out.println("Skipped without password: " + summary.skippedWithoutPassword());
        System.out.println("Failed imports: " + summary.failedImports());

        if (!summary.errors().isEmpty()) {
            System.out.println("Migration issues:");
            summary.errors().forEach(error -> System.out.println(" - " + error));
        }

        if (exitAfterRun) {
            int exitCode = summary.failedImports() > 0 ? 1 : 0;
            SpringApplication.exit(applicationContext, () -> exitCode);
            System.exit(exitCode);
        }
    }
}
