package com.aidonormatcher.backend.service;

import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.Role;
import com.aidonormatcher.backend.repository.NgoRepository;
import com.aidonormatcher.backend.repository.ReportRepository;
import com.aidonormatcher.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private NgoRepository ngoRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private ReportService reportService;

    // ─── submitReport ────────────────────────────────────────────────────────

    @Test
    void submitReport_savesReportAndEmailsReporter() {
        User reporter = User.builder().id(1L).email("donor@example.com").role(Role.DONOR).build();
        Ngo ngo = Ngo.builder().id(10L).name("Bad NGO").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(ngoRepository.findById(10L)).thenReturn(Optional.of(ngo));
        when(reportRepository.countByNgo(ngo)).thenReturn(1L); // below threshold

        reportService.submitReport(10L, "Suspected fraud", 1L);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getReason()).isEqualTo("Suspected fraud");
        assertThat(saved.getReporter()).isEqualTo(reporter);
        assertThat(saved.getNgo()).isEqualTo(ngo);
        verify(emailService).sendReportReceivedEmail(reporter);
    }

    @Test
    void submitReport_thirdReport_alertsAdmin() {
        User reporter = User.builder().id(1L).email("donor@example.com").role(Role.DONOR).build();
        Ngo ngo = Ngo.builder().id(10L).name("Bad NGO").build();
        User admin = User.builder().id(99L).email("admin@system.com").role(Role.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(ngoRepository.findById(10L)).thenReturn(Optional.of(ngo));
        when(reportRepository.countByNgo(ngo)).thenReturn(3L); // at threshold
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        reportService.submitReport(10L, "Another fraud claim", 1L);

        verify(emailService).sendAdminReportFlagEmail(admin, ngo);
    }

    @Test
    void submitReport_userNotFound_throwsRuntimeException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.submitReport(10L, "reason", 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
