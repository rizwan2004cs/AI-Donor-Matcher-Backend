package com.aidonormatcher.backend.controller;

import com.aidonormatcher.backend.dto.NeedRequest;
import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
import com.aidonormatcher.backend.service.AdminService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/ngos/pending")
    public ResponseEntity<List<Ngo>> getPendingNgos() {
        return ResponseEntity.ok(adminService.getPendingNgos());
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<Ngo>> getAllNgos() {
        return ResponseEntity.ok(adminService.getAllNgos());
    }

    @PostMapping("/ngos/{id}/approve")
    public ResponseEntity<String> approveNgo(@PathVariable Long id) {
        adminService.approveNgo(id);
        return ResponseEntity.ok("NGO approved.");
    }

    @PostMapping("/ngos/{id}/reject")
    public ResponseEntity<String> rejectNgo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        adminService.rejectNgo(id, body.get("reason"));
        return ResponseEntity.ok("NGO rejected.");
    }

    @PostMapping("/ngos/{id}/suspend")
    public ResponseEntity<String> suspendNgo(@PathVariable Long id) {
        adminService.suspendNgo(id);
        return ResponseEntity.ok("NGO suspended.");
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getReports() {
        return ResponseEntity.ok(adminService.getReports());
    }

    @PutMapping("/needs/{id}")
    public ResponseEntity<Need> editNeed(
            @PathVariable Long id,
            @Valid @RequestBody NeedRequest request) {
        return ResponseEntity.ok(adminService.editNeed(id, request));
    }

    @DeleteMapping("/needs/{id}")
    public ResponseEntity<Void> removeNeed(@PathVariable Long id) {
        adminService.removeNeed(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
