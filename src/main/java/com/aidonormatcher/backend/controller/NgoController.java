package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.MessageResponse;
import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.dto.ReportRequest;
import com.aidonormatcher.backend.dto.UrlResponse;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.CloudinaryService;
import com.aidonormatcher.backend.service.NgoService;
import com.aidonormatcher.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "NGO", description = "NGO profile management, discovery, and reporting endpoints.")
public class NgoController {

    private final NgoService ngoService;
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;

    @Operation(summary = "Get the authenticated NGO profile")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ngoService.getMyProfile(user.getEmail()));
    }

    @Operation(summary = "Update the authenticated NGO profile")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NgoProfileRequest request) {
        return ResponseEntity.ok(ngoService.updateProfile(user.getEmail(), request));
    }

    @Operation(summary = "Upload or replace the authenticated NGO photo")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/ngo/my/photo")
    public ResponseEntity<UrlResponse> uploadPhoto(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Image file to upload")
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadPhoto(file);
        ngoService.updatePhotoUrl(user.getEmail(), url);
        return ResponseEntity.ok(new UrlResponse(url));
    }

    @Operation(summary = "Discover approved NGOs")
    @GetMapping("/api/ngos")
    public ResponseEntity<List<NgoDiscoveryDTO>> discoverNgos(
            @Parameter(description = "Current latitude for distance calculations") @RequestParam(required = false) Double lat,
            @Parameter(description = "Current longitude for distance calculations") @RequestParam(required = false) Double lng,
            @Parameter(description = "Search radius in kilometers") @RequestParam(required = false) Double radius,
            @Parameter(description = "Filter by NGO category of work") @RequestParam(required = false) String category,
            @Parameter(description = "Search by NGO name") @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ngoService.discoverNgos(lat, lng, radius, category, search));
    }

    @Operation(summary = "Get a single NGO by id")
    @GetMapping("/api/ngos/{id}")
    public ResponseEntity<Ngo> getNgoById(@PathVariable Long id) {
        return ResponseEntity.ok(ngoService.getNgoById(id));
    }

    @Operation(summary = "Submit a report against an NGO")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/ngos/{id}/report")
    public ResponseEntity<MessageResponse> submitReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        reportService.submitReport(id, request.reason(), user.getId());
        return ResponseEntity.ok(new MessageResponse("Report submitted."));
    }
}
