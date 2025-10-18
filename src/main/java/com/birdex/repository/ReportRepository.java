package com.birdex.repository;

import com.birdex.entity.ReportEntity;
import com.birdex.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    Page<ReportEntity> findBySighting_SightingIdOrderByReportedAtDesc(UUID sightingId, Pageable pageable);

    long countByStatus(ReportStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReportEntity r set r.readAt = :readAt where r.id = :id")
    int markRead(@Param("id") UUID id, @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReportEntity r set r.status = :status where r.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") ReportStatus status);

    boolean existsBySighting_SightingId(UUID sightingId);

    @Query("""
        select r from ReportEntity r
        where (:status is null or r.status = :status)
        order by r.reportedAt desc
        """)
    Page<ReportEntity> findAllByStatusOrderByReportedAtDesc(
            @Param("status") ReportStatus status,
            Pageable pageable
    );
}