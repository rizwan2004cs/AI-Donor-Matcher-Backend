package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.IncomingPledgeResponse;
import com.aidonormatcher.backend.dto.NgoDetailResponse;
import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.dto.UrlResponse;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NeedCategory;
import com.aidonormatcher.backend.enums.NgoStatus;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.enums.TrustTier;
import com.aidonormatcher.backend.service.CloudinaryService;
import com.aidonormatcher.backend.service.NgoService;
import com.aidonormatcher.backend.service.PledgeService;
import com.aidonormatcher.backend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NgoControllerTest {

    @Mock
    private NgoService ngoService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ReportService reportService;

    @Mock
    private PledgeService pledgeService;

    @InjectMocks
    private NgoController ngoController;

    @Test
    void getMyProfile_returnsNgoForAuthenticatedNgoUser() {
        User ngoUser = ngoUser();
        Ngo ngo = sampleNgo(ngoUser);

        when(ngoService.getMyProfile("ngo@example.com")).thenReturn(ngo);

        ResponseEntity<Ngo> response = ngoController.getMyProfile(ngoUser);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(ngo);
        verify(ngoService).getMyProfile("ngo@example.com");
    }

    @Test
    void updateProfile_returnsUpdatedNgo() {
        User ngoUser = ngoUser();
        NgoProfileRequest request = new NgoProfileRequest(
                "Helping Hands",
                "123 Hope Street",
                "contact@ngo.org",
                "+9411222333",
                "This NGO supports vulnerable families with food, medicine, and shelter assistance.",
                NeedCategory.FOOD);
        Ngo updatedNgo = sampleNgo(ngoUser);

        when(ngoService.updateProfile(eq("ngo@example.com"), any(NgoProfileRequest.class))).thenReturn(updatedNgo);

        ResponseEntity<Ngo> response = ngoController.updateProfile(ngoUser, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedNgo);
        verify(ngoService).updateProfile("ngo@example.com", request);
    }

    @Test
    void uploadPhoto_returnsUploadedUrlAndUpdatesNgoPhoto() throws IOException {
        User ngoUser = ngoUser();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ngo-photo.jpg",
                "image/jpeg",
                "image-data".getBytes());

        when(cloudinaryService.uploadPhoto(file)).thenReturn("https://cdn.example.com/ngo-photo.jpg");
        doNothing().when(ngoService).updatePhotoUrl("ngo@example.com", "https://cdn.example.com/ngo-photo.jpg");

        ResponseEntity<UrlResponse> response = ngoController.uploadPhoto(ngoUser, file);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new UrlResponse("https://cdn.example.com/ngo-photo.jpg"));
        verify(cloudinaryService).uploadPhoto(file);
        verify(ngoService).updatePhotoUrl("ngo@example.com", "https://cdn.example.com/ngo-photo.jpg");
    }

    @Test
    void receivePledge_returnsUpdatedIncomingPledgeSummary() {
        User ngoUser = ngoUser();
        IncomingPledgeResponse responseBody = new IncomingPledgeResponse(
                44L, 12L, "Alice Donor", "alice@example.com", "Rice packs", "FOOD", 10,
                "FULFILLED", null, null, 22, 15, 15, 7);

        when(pledgeService.receivePledge(44L, 7L)).thenReturn(responseBody);

        ResponseEntity<IncomingPledgeResponse> response = ngoController.receivePledge(ngoUser, 44L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
        verify(pledgeService).receivePledge(44L, 7L);
    }

    @Test
    void getNgoById_returnsNgoDetailResponse() {
        NgoDetailResponse detail = new NgoDetailResponse(
                10L,
                "Helping Hands",
                "123 Hope Street",
                "contact@ngo.org",
                "+9411222333",
                "This NGO supports vulnerable families with food, medicine, and shelter assistance.",
                "FOOD",
                "https://cdn.example.com/ngo-photo.jpg",
                88,
                "TRUSTED",
                null,
                6.9271,
                79.8612,
                List.of()
        );

        when(ngoService.getNgoById(10L)).thenReturn(detail);

        ResponseEntity<NgoDetailResponse> response = ngoController.getNgoById(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(detail);
        verify(ngoService).getNgoById(10L);
    }

    private User ngoUser() {
        return User.builder()
                .id(7L)
                .fullName("Helping Hands User")
                .email("ngo@example.com")
                .password("encoded")
                .role(Role.NGO)
                .emailVerified(true)
                .build();
    }

    private Ngo sampleNgo(User ngoUser) {
        return Ngo.builder()
                .id(10L)
                .user(ngoUser)
                .name("Helping Hands")
                .address("123 Hope Street")
                .contactEmail("contact@ngo.org")
                .contactPhone("+9411222333")
                .description("This NGO supports vulnerable families with food, medicine, and shelter assistance.")
                .categoryOfWork(NeedCategory.FOOD)
                .photoUrl("https://cdn.example.com/ngo-photo.jpg")
                .documentUrl("https://cdn.example.com/ngo-doc.pdf")
                .status(NgoStatus.APPROVED)
                .profileComplete(true)
                .lat(6.9271)
                .lng(79.8612)
                .trustScore(88)
                .trustTier(TrustTier.TRUSTED)
                .build();
    }
}
