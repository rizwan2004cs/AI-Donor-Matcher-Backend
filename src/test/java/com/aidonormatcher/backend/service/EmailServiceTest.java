package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@aidonormatcher.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");
    }

    @Test
    void sendVerificationEmail_sendsEmailWithVerificationLink() {
        User user = User.builder()
                .id(1L).fullName("Alice").email("alice@example.com").build();

        emailService.sendVerificationEmail(user, "token-abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("alice@example.com");
        assertThat(msg.getSubject()).contains("Verify");
        assertThat(msg.getText()).contains("token-abc");
    }

    @Test
    void sendNgoApprovedEmail_sendsEmailToNgoContactEmail() {
        Ngo ngo = Ngo.builder().id(1L).name("Food Bank").contactEmail("food@bank.org").build();

        emailService.sendNgoApprovedEmail(ngo);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("food@bank.org");
    }

    @Test
    void sendNeedExpiryWarning_sendsEmailToNgo() {
        Ngo ngo = Ngo.builder().id(1L).contactEmail("ngo@org.com").build();
        Need need = Need.builder().id(10L).itemName("Water").build();

        emailService.sendNeedExpiryWarning(ngo, need);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("ngo@org.com");
        assertThat(captor.getValue().getText()).contains("Water");
    }
}
