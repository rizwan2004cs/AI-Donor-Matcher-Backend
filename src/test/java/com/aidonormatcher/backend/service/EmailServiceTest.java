package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceTest {

    private EmailService emailService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://api.resend.com").build();

        emailService = new EmailService(restClient);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@aidonormatcher.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "emailProvider", "resend");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "re_test_key");
    }

    @Test
    void sendVerificationEmail_sendsEmailWithVerificationLink() {
        User user = User.builder()
                .id(1L).fullName("Alice").email("alice@example.com").build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from":"no-reply@aidonormatcher.com",
                          "to":["alice@example.com"],
                          "subject":"Verify your AI Donor Matcher account"
                        }
                        """, false))
                .andExpect(content().string(containsString("token-abc")))
                .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

        emailService.sendVerificationEmail(user, "token-abc");

        server.verify();
    }

    @Test
    void sendNgoApprovedEmail_sendsEmailToNgoContactEmail() {
        Ngo ngo = Ngo.builder().id(1L).name("Food Bank").contactEmail("food@bank.org").build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("food@bank.org")))
                .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

        emailService.sendNgoApprovedEmail(ngo);

        server.verify();
    }

    @Test
    void sendNeedExpiryWarning_sendsEmailToNgo() {
        Ngo ngo = Ngo.builder().id(1L).contactEmail("ngo@org.com").build();
        Need need = Need.builder().id(10L).itemName("Water").build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("ngo@org.com")))
                .andExpect(content().string(containsString("Water")))
                .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

        emailService.sendNeedExpiryWarning(ngo, need);

        server.verify();
    }

    @Test
    void sendNgoApprovedEmail_whenRequestFails_queuesRetryAndDoesNotThrow() {
        Ngo ngo = Ngo.builder().id(1L).name("Food Bank").contactEmail("food@bank.org").build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withServerError());
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

        assertThatNoException().isThrownBy(() -> emailService.sendNgoApprovedEmail(ngo));
        emailService.processQueuedEmails();

        server.verify();
    }
}
