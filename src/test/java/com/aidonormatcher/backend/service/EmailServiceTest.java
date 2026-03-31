package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private Resend resend;

    @Mock
    private Emails resendEmails;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(resend.emails()).thenReturn(resendEmails);

        emailService = new EmailService(resend);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@aidonormatcher.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "emailProvider", "resend");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "test-api-key");
    }

    @Test
    void sendVerificationEmail_sendsEmailWithVerificationLink() throws ResendException {
        User user = User.builder()
                .id(1L)
                .fullName("Alice")
                .email("alice@example.com")
                .build();

        when(resendEmails.send(any(CreateEmailOptions.class))).thenReturn(org.mockito.Mockito.mock(CreateEmailResponse.class));

        emailService.sendVerificationEmail(user, "token-abc");

        ArgumentCaptor<CreateEmailOptions> emailCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(resendEmails).send(emailCaptor.capture());

        CreateEmailOptions sentEmail = emailCaptor.getValue();
        assertThat(sentEmail.getFrom()).isEqualTo("no-reply@aidonormatcher.com");
        assertThat(sentEmail.getTo()).containsExactly("alice@example.com");
        assertThat(sentEmail.getSubject()).isEqualTo("Verify your email for AI Donor Matcher");
        assertThat(sentEmail.getText()).contains("token-abc");
    }

    @Test
    void sendNgoApprovedEmail_sendsEmailToNgoContactEmail() throws ResendException {
        Ngo ngo = Ngo.builder()
                .id(1L)
                .name("Food Bank")
                .contactEmail("food@bank.org")
                .build();

        when(resendEmails.send(any(CreateEmailOptions.class))).thenReturn(org.mockito.Mockito.mock(CreateEmailResponse.class));

        emailService.sendNgoApprovedEmail(ngo);

        ArgumentCaptor<CreateEmailOptions> emailCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(resendEmails).send(emailCaptor.capture());

        CreateEmailOptions sentEmail = emailCaptor.getValue();
        assertThat(sentEmail.getTo()).containsExactly("food@bank.org");
        assertThat(sentEmail.getSubject()).isEqualTo("Your NGO application has been approved");
        assertThat(sentEmail.getText()).contains("complete or review your profile");
    }

    @Test
    void sendNeedExpiryWarning_sendsEmailToNgo() throws ResendException {
        Ngo ngo = Ngo.builder()
                .id(1L)
                .contactEmail("ngo@org.com")
                .build();
        Need need = Need.builder()
                .id(10L)
                .itemName("Water")
                .build();

        when(resendEmails.send(any(CreateEmailOptions.class))).thenReturn(org.mockito.Mockito.mock(CreateEmailResponse.class));

        emailService.sendNeedExpiryWarning(ngo, need);

        ArgumentCaptor<CreateEmailOptions> emailCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(resendEmails).send(emailCaptor.capture());

        CreateEmailOptions sentEmail = emailCaptor.getValue();
        assertThat(sentEmail.getTo()).containsExactly("ngo@org.com");
        assertThat(sentEmail.getText()).contains("Water");
    }

    @Test
    void sendNgoApprovedEmail_whenRequestFails_queuesRetryAndDoesNotThrow() throws ResendException {
        Ngo ngo = Ngo.builder()
                .id(1L)
                .name("Food Bank")
                .contactEmail("food@bank.org")
                .build();

        when(resendEmails.send(any(CreateEmailOptions.class)))
                .thenThrow(new ResendException("quota exceeded"))
                .thenReturn(org.mockito.Mockito.mock(CreateEmailResponse.class));

        assertThatNoException().isThrownBy(() -> emailService.sendNgoApprovedEmail(ngo));
        emailService.processQueuedEmails();

        verify(resendEmails, times(2)).send(any(CreateEmailOptions.class));
    }
}
