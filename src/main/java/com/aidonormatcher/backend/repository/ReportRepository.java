package com.aidonormatcher.backend.repository;

import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByNgo(Ngo ngo);

    List<Report> findAllByOrderByReportedAtDesc();
}
