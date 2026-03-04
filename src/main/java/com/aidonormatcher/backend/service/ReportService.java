package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.ReportRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final EmailService emailService;

    @Transactional
    public void submitReport(Long ngoId, String reason, Long reporterUserId) {
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new RuntimeException("User not found."));
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found."));

        Report report = Report.builder()
                .reporter(reporter)
                .ngo(ngo)
                .reason(reason)
                .reportedAt(LocalDateTime.now())
                .build();
        reportRepository.save(report);

        emailService.sendReportReceivedEmail(reporter);

        // Auto-flag if 3 or more reports
        long reportCount = reportRepository.countByNgo(ngo);
        if (reportCount >= 3) {
            User admin = userRepository.findByRole(Role.ADMIN).get(0);
            emailService.sendAdminReportFlagEmail(admin, ngo);
        }
    }
}
