package com.aidonormatcher.backend.repository;

import com.aidonormatcher.backend.entity.Ngo;
import com.aidonormatcher.backend.entity.User;
import com.aidonormatcher.backend.enums.NgoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NgoRepository extends JpaRepository<Ngo, Long> {

    Optional<Ngo> findByUser(User user);

    Optional<Ngo> findByUserId(Long userId);

    List<Ngo> findByStatus(NgoStatus status);

    Page<Ngo> findByStatus(NgoStatus status, Pageable pageable);

    @Query(value = """
            SELECT *
            FROM (
                SELECT n.*,
                (6371 * acos(
                    cos(radians(:lat)) * cos(radians(n.lat)) *
                    cos(radians(n.lng) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(n.lat))
                )) AS distance_km
                FROM ngos n
                WHERE n.status = 'APPROVED'
                  AND n.profile_complete = true
                  AND (:category IS NULL OR n.category_of_work = :category)
                  AND (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ) nearby_ngos
            WHERE nearby_ngos.distance_km <= :radius
            ORDER BY nearby_ngos.distance_km ASC
            """, nativeQuery = true)
    List<Object[]> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius,
            @Param("category") String category,
            @Param("search") String search
    );

    @Query(value = """
            SELECT n.*
            FROM ngos n
            WHERE n.status = 'APPROVED'
              AND n.profile_complete = true
              AND (:category IS NULL OR n.category_of_work = :category)
              AND (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY n.trust_score DESC
            """, nativeQuery = true)
    List<Ngo> findAllLive(
            @Param("category") String category,
            @Param("search") String search
    );
}
