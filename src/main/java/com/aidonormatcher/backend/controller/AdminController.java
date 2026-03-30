package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.AdminNgoSummaryResponse;
import com.aidonormatcher.backend.dto.AdminReportSummaryResponse;
import com.aidonormatcher.backend.dto.MessageResponse;
import com.aidonormatcher.backend.dto.NeedDetailResponse;
import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.dto.ReasonRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin moderation, reporting, and dashboard endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "List NGOs awaiting approval")
    @GetMapping("/ngos/pending")
    public ResponseEntity<List<AdminNgoSummaryResponse>> getPendingNgos(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminService.getPendingNgos(limit));
    }

    @Operation(summary = "List all NGO profiles")
    @GetMapping("/ngos")
    public ResponseEntity<List<AdminNgoSummaryResponse>> getAllNgos(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminService.getAllNgos(limit));
    }

    @Operation(summary = "List all needs belonging to a specific NGO")
    @GetMapping("/ngos/{id}/needs")
    public ResponseEntity<List<NeedDetailResponse>> getNgoNeeds(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getNgoNeeds(id));
    }

    @Operation(summary = "Approve an NGO")
    @PostMapping("/ngos/{id}/approve")
    public ResponseEntity<MessageResponse> approveNgo(@PathVariable Long id) {
        adminService.approveNgo(id);
        return ResponseEntity.ok(new MessageResponse("NGO approved."));
    }

    @Operation(summary = "Reject an NGO with a reason")
    @PostMapping("/ngos/{id}/reject")
    public ResponseEntity<MessageResponse> rejectNgo(
            @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {
        adminService.rejectNgo(id, request.reason());
        return ResponseEntity.ok(new MessageResponse("NGO rejected."));
    }

    @Operation(summary = "Suspend an NGO and expire active needs")
    @PostMapping("/ngos/{id}/suspend")
    public ResponseEntity<MessageResponse> suspendNgo(@PathVariable Long id) {
        adminService.suspendNgo(id);
        return ResponseEntity.ok(new MessageResponse("NGO suspended."));
    }

    @Operation(summary = "List NGO reports submitted by donors")
    @GetMapping("/reports")
    public ResponseEntity<List<AdminReportSummaryResponse>> getReports(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(adminService.getReports(limit));
    }

    @Operation(summary = "Edit a need as an administrator")
    @PutMapping("/needs/{id}")
    public ResponseEntity<Need> editNeed(
            @PathVariable Long id,
            @Valid @RequestBody NeedRequest request) {
        return ResponseEntity.ok(adminService.editNeed(id, request));
    }

    @Operation(summary = "Delete a need as an administrator")
    @DeleteMapping("/needs/{id}")
    public ResponseEntity<Void> removeNeed(@PathVariable Long id) {
        adminService.removeNeed(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get admin dashboard statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
