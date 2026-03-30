package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NgoDiscoveryDTO;
import com.aidonormatcher.backend.dto.NgoProfileRequest;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.CloudinaryService;
import com.aidonormatcher.backend.service.NgoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NgoController {

    private final NgoService ngoService;
    private final CloudinaryService cloudinaryService;

    // ─── NGO: Own Profile ────────────────────────────────────────────────────

    @GetMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ngoService.getMyProfile(user.getEmail()));
    }

    @PutMapping("/api/ngo/my/profile")
    public ResponseEntity<Ngo> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NgoProfileRequest request) {
        return ResponseEntity.ok(ngoService.updateProfile(user.getEmail(), request));
    }

    @PostMapping("/api/ngo/my/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadPhoto(file);
        ngoService.updatePhotoUrl(user.getEmail(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/api/ngos")
    public ResponseEntity<List<NgoDiscoveryDTO>> discoverNgos(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ngoService.discoverNgos(lat, lng, radius, category, search));
    }

    @GetMapping("/api/ngos/{id}")
    public ResponseEntity<Ngo> getNgoById(@PathVariable Long id) {
        return ResponseEntity.ok(ngoService.getNgoById(id));
    }
}
