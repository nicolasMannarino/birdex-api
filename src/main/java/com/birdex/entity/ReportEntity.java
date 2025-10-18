package com.birdex.entity;


import com.birdex.entity.converter.ReportStatusConverter;
import com.birdex.entity.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_reports_sighting", columnList = "sighting_id"),
                @Index(name = "idx_reports_status", columnList = "status"),
                @Index(name = "idx_reports_reported_at", columnList = "reported_at"),
                @Index(name = "idx_reports_reported_by", columnList = "reported_by_user_id")
        }
)
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sighting_id", nullable = false)
    private SightingEntity sighting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private UserEntity reportedBy;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Convert(converter = ReportStatusConverter.class)
    @Column(name = "status", nullable = false)
    private ReportStatus status;

    @Column(name = "description")
    private String description;

    @PrePersist
    void prePersist() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }
}