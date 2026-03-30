package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.service.NeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Needs", description = "NGO need management endpoints.")
public class NeedController {

    private final NeedService needService;
    private final NgoRepository ngoRepository;

    @Operation(summary = "Get a single need by id")
    @GetMapping("/api/needs/{id}")
    public ResponseEntity<NeedDetailResponse> getNeedDetail(@PathVariable Long id) {
        return ResponseEntity.ok(needService.getNeedDetail(id));
    }

    @Operation(summary = "List needs created by the authenticated NGO")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/ngo/my/needs")
    public ResponseEntity<List<Need>> getMyNeeds(@AuthenticationPrincipal User user) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.getNeedsByNgo(ngo));
    }

    @Operation(summary = "Create a new need")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/needs")
    public ResponseEntity<Need> createNeed(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NeedRequest request) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.createNeed(request, ngo.getId()));
    }

    @Operation(summary = "Update an existing need")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/needs/{id}")
    public ResponseEntity<Need> updateNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody NeedRequest request) {
        return ResponseEntity.ok(needService.updateNeed(id, request, user.getId()));
    }

    @Operation(summary = "Delete a need")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/needs/{id}")
    public ResponseEntity<Void> deleteNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.deleteNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark a need as fulfilled")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/api/needs/{id}/fulfill")
    public ResponseEntity<Void> fulfillNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.fulfillNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
