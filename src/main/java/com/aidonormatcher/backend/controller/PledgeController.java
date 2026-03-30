package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.PledgeDetailResponse;
import com.aidonormatcher.backend.dto.PledgeRequest;
import com.aidonormatcher.backend.dto.PledgeResponse;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.service.PledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pledges")
@RequiredArgsConstructor
@Tag(name = "Pledges", description = "Donor pledge creation and history endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class PledgeController {

    private final PledgeService pledgeService;

    @Operation(summary = "Create a pledge against a need")
    @PostMapping
    public ResponseEntity<PledgeResponse> createPledge(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PledgeRequest request) {
        return ResponseEntity.ok(pledgeService.createPledge(request, user.getId()));
    }

    @Operation(summary = "Get a pledge by id for delivery-view refresh")
    @GetMapping("/{id}")
    public ResponseEntity<PledgeDetailResponse> getPledgeDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(pledgeService.getPledgeDetails(id, user.getId()));
    }

    @Operation(summary = "Cancel an active pledge")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPledge(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        pledgeService.cancelPledge(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List active pledges for the authenticated donor")
    @GetMapping("/active")
    public ResponseEntity<List<Pledge>> getActivePledges(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getActivePledges(user.getId()));
    }

    @Operation(summary = "List pledge history for the authenticated donor")
    @GetMapping("/history")
    public ResponseEntity<List<Pledge>> getPledgeHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pledgeService.getPledgeHistory(user.getId()));
    }
}
