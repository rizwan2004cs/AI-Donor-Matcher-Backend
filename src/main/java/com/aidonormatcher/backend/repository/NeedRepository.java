package com.aidonormatcher.backend.repository;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.enums.NeedStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NeedRepository extends JpaRepository<Need, Long> {

    List<Need> findByNgo(Ngo ngo);

    long countByNgoAndStatusIn(Ngo ngo, List<NeedStatus> statuses);

    long countByNgoAndStatus(Ngo ngo, NeedStatus status);

    List<Need> findByNgoAndStatusIn(Ngo ngo, List<NeedStatus> statuses);

    Need findTopByNgoAndStatusInOrderByUrgencyDescCreatedAtAsc(Ngo ngo, List<NeedStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Need n WHERE n.id = :id")
    Need findByIdWithLock(@Param("id") Long id);

    List<Need> findByExpiryDateAndStatusIn(LocalDate expiryDate, List<NeedStatus> statuses);

    List<Need> findByExpiryDateBeforeAndStatusIn(LocalDate date, List<NeedStatus> statuses);

    long countByStatusIn(List<NeedStatus> statuses);

    long countByFulfilledAtBetween(LocalDateTime start, LocalDateTime end);
}
