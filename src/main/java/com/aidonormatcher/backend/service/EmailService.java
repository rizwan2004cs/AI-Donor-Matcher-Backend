package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestClient resendRestClient;
    private final Queue<QueuedEmail> retryQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_RETRY_ATTEMPTS = 6;

    @Value("${mail.from}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${email.provider:resend}")
    private String emailProvider;

    @Value("${email.resend.api-key}")
    private String resendApiKey;

    private String getNgoRecipientEmail(Ngo ngo) {
        if (ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()) {
            return ngo.getContactEmail();
        }
        return ngo.getUser() != null ? ngo.getUser().getEmail() : null;
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }

        sendWithRetry(new QueuedEmail(to, subject, body, 0));
    }

    private void sendWithRetry(QueuedEmail email) {
        try {
            sendViaConfiguredProvider(email);
        } catch (RestClientException ex) {
            enqueueForRetry(email.nextAttempt());
        }
    }

    private void sendViaConfiguredProvider(QueuedEmail email) {
        if (!"resend".equalsIgnoreCase(emailProvider)) {
            throw new IllegalStateException("Unsupported email provider: " + emailProvider);
        }

        resendRestClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResendEmailRequest(
                        fromEmail,
                        List.of(email.to()),
                        email.subject(),
                        email.body()))
                .retrieve()
                .toBodilessEntity();
    }

    private void enqueueForRetry(QueuedEmail email) {
        if (email.attempts() > MAX_RETRY_ATTEMPTS) {
            return;
        }

        retryQueue.offer(email);
    }

    @Scheduled(fixedDelay = 900_000)
    public void processQueuedEmails() {
        int queuedCount = retryQueue.size();
        for (int i = 0; i < queuedCount; i++) {
            QueuedEmail queuedEmail = retryQueue.poll();
            if (queuedEmail == null) {
                return;
            }

            sendWithRetry(queuedEmail);
        }
    }

    public void sendVerificationEmail(User user, String token) {
        String link = baseUrl + "/api/auth/verify?token=" + token;
        send(user.getEmail(), "Verify your AI Donor Matcher account",
                "Hi " + user.getFullName() + ",\n\nClick to verify:\n" + link);
    }

    public void sendVerificationOtpEmail(User user, String otp) {
        String body = "Hi " + user.getFullName() + ",\n\n"
                + "Your AI Donor Matcher verification code is " + otp + ".\n"
                + "This code is valid for 10 minutes.\n";
        send(user.getEmail(), "Your AI Donor Matcher verification code", body);
    }

    public void sendPreRegistrationOtpEmail(String email, String otp) {
        String body = "Hi,\n\n"
                + "Your AI Donor Matcher registration verification code is " + otp + ".\n"
                + "This code is valid for 10 minutes.\n";
        send(email, "Your AI Donor Matcher verification code", body);
    }

    public void sendNgoApprovedEmail(Ngo ngo) {
        send(getNgoRecipientEmail(ngo), "Your NGO application has been approved",
                "Congratulations! Complete your profile to go live on the map: "
                        + baseUrl + "/dashboard/ngo/complete-profile");
    }

    public void sendNgoRejectedEmail(Ngo ngo, String reason) {
        send(getNgoRecipientEmail(ngo), "Your NGO application was not approved",
                "Reason: " + reason + "\n\nYou may reapply with corrected documents.");
    }

    public void sendNgoSuspendedEmail(Ngo ngo) {
        send(getNgoRecipientEmail(ngo), "Your NGO account has been suspended",
                "Your NGO account has been suspended. Please contact the platform administrator for details.");
    }

    public void sendPledgeConfirmationEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Pledge confirmed â€” " + pledge.getNeed().getItemName(),
                "Thank you! You pledged " + pledge.getQuantity() + " x "
                        + pledge.getNeed().getItemName()
                        + "\n\nDeliver to: " + ngo.getAddress()
                        + "\nContact: " + ngo.getContactEmail()
                        + "\nYour pledge expires in 48 hours.");
    }

    public void sendPledgeCancelledByDonorEmail(Ngo ngo, Pledge pledge) {
        send(getNgoRecipientEmail(ngo), "A pledge was cancelled",
                pledge.getDonor().getFullName() + " cancelled their pledge of "
                        + pledge.getQuantity() + " x " + pledge.getNeed().getItemName() + ".");
    }

    public void sendPledgeExpiredEmail(User donor, Pledge pledge) {
        send(donor.getEmail(), "Your pledge has expired",
                "Your pledge of " + pledge.getQuantity() + " x "
                        + pledge.getNeed().getItemName() + " has expired after 48 hours. "
                        + "The item is still available â€” you can pledge again.");
    }

    public void sendFulfillmentThankYouEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Thank you for your donation!",
                "Your donation of " + pledge.getQuantity() + " x "
                        + pledge.getNeed().getItemName() + " to " + ngo.getName()
                        + " has been marked as received. Thank you!");
    }

    public void sendNgoApplicationAlert(User admin, Ngo ngo) {
        send(admin.getEmail(), "New NGO application: " + ngo.getName(),
                "A new NGO has applied. Review at: " + baseUrl + "/dashboard/admin");
    }

    public void sendNeedExpiryWarning(Ngo ngo, Need need) {
        send(getNgoRecipientEmail(ngo), "Need expiring soon: " + need.getItemName(),
                "Your need for " + need.getItemName() + " expires on " + need.getExpiryDate() + ".");
    }

    public void sendNeedAutoExpiredEmail(Ngo ngo, Need need) {
        send(getNgoRecipientEmail(ngo), "Need closed: " + need.getItemName(),
                "Your need for " + need.getItemName() + " has expired and been closed.");
    }

    public void sendReportReceivedEmail(User reporter) {
        send(reporter.getEmail(), "Report received",
                "Thank you. Your report has been received and will be reviewed by our team.");
    }

    public void sendAdminReportFlagEmail(User admin, Ngo ngo) {
        send(admin.getEmail(), "NGO flagged: " + ngo.getName(),
                ngo.getName() + " has received 3 or more reports. Review at: "
                        + baseUrl + "/dashboard/admin");
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String text) {
    }

    private record QueuedEmail(String to, String subject, String body, int attempts) {
        private QueuedEmail nextAttempt() {
            return new QueuedEmail(to, subject, body, attempts + 1);
        }
    }
}
