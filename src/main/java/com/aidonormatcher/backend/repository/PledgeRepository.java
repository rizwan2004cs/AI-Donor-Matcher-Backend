package com.aidonormatcher.backend.repository;

import com.aidonormatcher.backend.entity.Need;
import com.aidonormatcher.backend.entity.Pledge;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.PledgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PledgeRepository extends JpaRepository<Pledge, Long> {

    List<Pledge> findByNeedAndStatus(Need need, PledgeStatus status);

    List<Pledge> findByDonorAndStatus(User donor, PledgeStatus status);

    List<Pledge> findByDonor(User donor);

    List<Pledge> findByStatusAndCreatedAtBefore(PledgeStatus status, LocalDateTime cutoff);

    List<Pledge> findByNeedNgoIdAndStatus(Long ngoId, PledgeStatus status);

    List<Pledge> findByNeedNgoId(Long ngoId);

    List<Pledge> findByNeedNgoIdAndStatusOrderByCreatedAtDesc(Long ngoId, PledgeStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
