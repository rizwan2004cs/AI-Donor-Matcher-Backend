package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int MAX_RETRY_ATTEMPTS = 6;

    private final Resend resend;
    private final Queue<QueuedEmail> retryQueue = new ConcurrentLinkedQueue<>();

    @Value("${mail.from}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${email.provider:resend}")
    private String emailProvider;

    @Value("${email.resend.api-key:}")
    private String resendApiKey;

    private String getNgoRecipientEmail(Ngo ngo) {
        if (ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()) {
            return ngo.getContactEmail();
        }
        return ngo.getUser() != null ? ngo.getUser().getEmail() : null;
    }

    private String getNgoDisplayName(Ngo ngo) {
        if (ngo.getName() != null && !ngo.getName().isBlank()) {
            return ngo.getName();
        }
        return "your NGO";
    }

    private String buildMessage(String recipientName, String... paragraphs) {
        StringBuilder message = new StringBuilder();

        if (recipientName == null || recipientName.isBlank()) {
            message.append("Hello,");
        } else {
            message.append("Hello ").append(recipientName).append(",");
        }

        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            message.append("\n\n").append(paragraph);
        }

        message.append("\n\nRegards,\nAI Donor Matcher Team");
        return message.toString();
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }

        if (!isEmailDeliveryConfigured()) {
            return;
        }

        sendWithRetry(new QueuedEmail(to, subject, body, 0));
    }

    private void sendWithRetry(QueuedEmail email) {
        try {
            sendViaConfiguredProvider(email);
        } catch (ResendException ex) {
            enqueueForRetry(email.nextAttempt());
        }
    }

    private void sendViaConfiguredProvider(QueuedEmail email) throws ResendException {
        if (!"resend".equalsIgnoreCase(emailProvider)) {
            return;
        }

        if (!isResendConfigured()) {
            return;
        }

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(email.to())
                .subject(email.subject())
                .text(email.body())
                .build();

        resend.emails().send(options);
    }

    private boolean isEmailDeliveryConfigured() {
        if ("resend".equalsIgnoreCase(emailProvider)) {
            return isResendConfigured();
        }
        return false;
    }

    private boolean isResendConfigured() {
        return resendApiKey != null
                && !resendApiKey.isBlank()
                && fromEmail != null
                && !fromEmail.isBlank();
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
        send(user.getEmail(), "Verify your email for AI Donor Matcher",
                buildMessage(user.getFullName(),
                        "We received a request to verify your AI Donor Matcher account.",
                        "To complete verification, open the link below:\n" + link,
                        "If you did not create this account, you can safely ignore this email."));
    }

    public void sendVerificationOtpEmail(User user, String otp) {
        send(user.getEmail(), "Your AI Donor Matcher OTP code",
                buildMessage(user.getFullName(),
                        "Use the verification code below to continue signing in to AI Donor Matcher.",
                        "Verification code: " + otp,
                        "This code is valid for 10 minutes. For your security, please do not share it with anyone.",
                        "If you did not request this code, you can ignore this email."));
    }

    public void sendPreRegistrationOtpEmail(String email, String otp) {
        send(email, "Complete your signup - AI Donor Matcher OTP",
                buildMessage(null,
                        "Thank you for starting your AI Donor Matcher registration.",
                        "Enter the code below on the OTP screen to complete your signup:\n" + otp,
                        "This code is valid for 10 minutes. Your account will only be created after successful verification.",
                        "If you did not request this code, you can ignore this email."));
    }

    public void sendNgoApprovedEmail(Ngo ngo) {
        send(getNgoRecipientEmail(ngo), "Your NGO application has been approved",
                buildMessage(getNgoDisplayName(ngo),
                        "Congratulations. Your NGO application has been approved by the AI Donor Matcher admin team.",
                        "To make your organization visible to donors, please complete or review your profile here:\n"
                                + baseUrl + "/dashboard/ngo/complete-profile",
                        "Once your profile is complete, your needs and location can appear in donor discovery flows."));
    }

    public void sendNgoRejectedEmail(Ngo ngo, String reason) {
        send(getNgoRecipientEmail(ngo), "Update on your NGO application",
                buildMessage(getNgoDisplayName(ngo),
                        "We reviewed your NGO application, but it could not be approved at this time.",
                        "Reason provided by the admin team:\n" + reason,
                        "You may update your supporting information and apply again when ready."));
    }

    public void sendNgoSuspendedEmail(Ngo ngo) {
        send(getNgoRecipientEmail(ngo), "Important update about your NGO account",
                buildMessage(getNgoDisplayName(ngo),
                        "Your NGO account has been suspended by the AI Donor Matcher admin team.",
                        "While suspended, your organization and active needs will not be shown to donors.",
                        "Please contact the platform administrator if you need clarification or next steps."));
    }

    public void sendPledgeConfirmationEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Pledge confirmed for " + pledge.getNeed().getItemName(),
                buildMessage(donor.getFullName(),
                        "Thank you for supporting " + getNgoDisplayName(ngo) + " through AI Donor Matcher.",
                        "Your pledge details:\n"
                                + "- Item: " + pledge.getNeed().getItemName() + "\n"
                                + "- Quantity: " + pledge.getQuantity(),
                        "Delivery details:\n"
                                + "- Address: " + ngo.getAddress() + "\n"
                                + "- Contact: " + ngo.getContactEmail(),
                        "Please deliver the pledged items within 48 hours before the pledge expires."));
    }

    public void sendPledgeCancelledByDonorEmail(Ngo ngo, Pledge pledge) {
        send(getNgoRecipientEmail(ngo), "A donor pledge has been cancelled",
                buildMessage(getNgoDisplayName(ngo),
                        pledge.getDonor().getFullName() + " cancelled a pledge linked to one of your active needs.",
                        "Cancelled pledge details:\n"
                                + "- Item: " + pledge.getNeed().getItemName() + "\n"
                                + "- Quantity: " + pledge.getQuantity(),
                        "Your need remains active if quantity is still outstanding."));
    }

    public void sendPledgeExpiredEmail(User donor, Pledge pledge) {
        send(donor.getEmail(), "Your pledge has expired",
                buildMessage(donor.getFullName(),
                        "Your pledge has expired because it was not completed within the delivery window.",
                        "Expired pledge details:\n"
                                + "- Item: " + pledge.getNeed().getItemName() + "\n"
                                + "- Quantity: " + pledge.getQuantity(),
                        "If the need is still open, you are welcome to pledge again from the donor dashboard."));
    }

    public void sendFulfillmentThankYouEmail(User donor, Pledge pledge, Ngo ngo) {
        send(donor.getEmail(), "Thank you for your donation",
                buildMessage(donor.getFullName(),
                        "Your delivery has been marked as received by " + getNgoDisplayName(ngo) + ".",
                        "Received pledge details:\n"
                                + "- Item: " + pledge.getNeed().getItemName() + "\n"
                                + "- Quantity: " + pledge.getQuantity(),
                        "Thank you for contributing through AI Donor Matcher and supporting this need."));
    }

    public void sendNgoApplicationAlert(User admin, Ngo ngo) {
        send(admin.getEmail(), "New NGO application awaiting review",
                buildMessage(admin.getFullName(),
                        "A new NGO application has been submitted on AI Donor Matcher.",
                        "Applicant: " + getNgoDisplayName(ngo),
                        "Review the application in the admin dashboard:\n" + baseUrl + "/dashboard/admin"));
    }

    public void sendNeedExpiryWarning(Ngo ngo, Need need) {
        send(getNgoRecipientEmail(ngo), "Need expiring soon: " + need.getItemName(),
                buildMessage(getNgoDisplayName(ngo),
                        "One of your active needs is approaching its expiry date.",
                        "Need details:\n"
                                + "- Item: " + need.getItemName() + "\n"
                                + "- Expiry date: " + need.getExpiryDate(),
                        "If the need is still valid, please review it in your dashboard and take any required action."));
    }

    public void sendNeedAutoExpiredEmail(Ngo ngo, Need need) {
        send(getNgoRecipientEmail(ngo), "Need automatically closed: " + need.getItemName(),
                buildMessage(getNgoDisplayName(ngo),
                        "Your need has reached its expiry date and has been closed automatically.",
                        "Closed need:\n"
                                + "- Item: " + need.getItemName() + "\n"
                                + "- Expiry date: " + need.getExpiryDate(),
                        "You can create a new need from your NGO dashboard if support is still required."));
    }

    public void sendReportReceivedEmail(User reporter) {
        send(reporter.getEmail(), "We received your report",
                buildMessage(reporter.getFullName(),
                        "Thank you for reporting this issue to AI Donor Matcher.",
                        "Your report has been recorded and will be reviewed by the admin team.",
                        "If additional information is required, the team may contact you later."));
    }

    public void sendAdminReportFlagEmail(User admin, Ngo ngo) {
        send(admin.getEmail(), "NGO flagged for admin review",
                buildMessage(admin.getFullName(),
                        getNgoDisplayName(ngo) + " has received three or more reports and now requires admin review.",
                        "Open the admin dashboard to review the case:\n" + baseUrl + "/dashboard/admin"));
    }

    private record QueuedEmail(String to, String subject, String body, int attempts) {
        private QueuedEmail nextAttempt() {
            return new QueuedEmail(to, subject, body, attempts + 1);
        }
    }
}
