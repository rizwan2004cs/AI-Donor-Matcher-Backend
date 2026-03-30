package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.service.NeedService;
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
public class NeedController {

    private final NeedService needService;
    private final NgoRepository ngoRepository;

    @GetMapping("/api/ngo/my/needs")
    public ResponseEntity<List<Need>> getMyNeeds(@AuthenticationPrincipal User user) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.getNeedsByNgo(ngo));
    }

    @PostMapping("/api/needs")
    public ResponseEntity<Need> createNeed(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NeedRequest request) {
        Ngo ngo = ngoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("NGO profile not found."));
        return ResponseEntity.ok(needService.createNeed(request, ngo.getId()));
    }

    @PutMapping("/api/needs/{id}")
    public ResponseEntity<Need> updateNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody NeedRequest request) {
        return ResponseEntity.ok(needService.updateNeed(id, request, user.getId()));
    }

    @DeleteMapping("/api/needs/{id}")
    public ResponseEntity<Void> deleteNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.deleteNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/needs/{id}/fulfill")
    public ResponseEntity<Void> fulfillNeed(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        needService.fulfillNeed(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
