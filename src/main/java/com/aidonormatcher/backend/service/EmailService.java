package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class EmailService {

        private final JavaMailSender mailSender;
        private final Queue<QueuedEmail> retryQueue = new ConcurrentLinkedQueue<>();
        private static final int MAX_RETRY_ATTEMPTS = 6;

        @Value("${spring.mail.username}")
        private String fromEmail;

        @Value("${app.base-url}")
        private String baseUrl;

        private String getNgoRecipientEmail(Ngo ngo) {
                if (ngo.getContactEmail() != null && !ngo.getContactEmail().isBlank()) {
                        return ngo.getContactEmail();
                }
                return ngo.getUser() != null ? ngo.getUser().getEmail() : null;
        }

        private void send(String to, String subject, String body) {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromEmail);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                sendWithRetry(msg, 0);
        }

        private void sendWithRetry(SimpleMailMessage msg, int attempts) {
                try {
                        mailSender.send(msg);
                } catch (MailException ex) {
                        enqueueForRetry(msg, attempts + 1);
                }
        }

        private void enqueueForRetry(SimpleMailMessage msg, int attempts) {
                if (attempts > MAX_RETRY_ATTEMPTS) {
                        return;
                }

                retryQueue.offer(new QueuedEmail(new SimpleMailMessage(msg), attempts));
        }

        @Scheduled(fixedDelay = 900_000)
        public void processQueuedEmails() {
                int queuedCount = retryQueue.size();
                for (int i = 0; i < queuedCount; i++) {
                        QueuedEmail queuedEmail = retryQueue.poll();
                        if (queuedEmail == null) {
                                return;
                        }

                        sendWithRetry(queuedEmail.message(), queuedEmail.attempts());
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
                                + "This code is valid for " + 10 + " minutes.\n";
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
                send(donor.getEmail(), "Pledge confirmed — " + pledge.getNeed().getItemName(),
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
                                                + "The item is still available — you can pledge again.");
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
                send(admin.getEmail(), "⚠️ NGO flagged: " + ngo.getName(),
                                ngo.getName() + " has received 3 or more reports. Review at: "
                                                + baseUrl + "/dashboard/admin");
        }
        private record QueuedEmail(SimpleMailMessage message, int attempts) {
        }
}
