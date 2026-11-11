package com.radiofind.track.report;

import com.radiofind.track.entity.Track;
import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_reports", uniqueConstraints = {
    @UniqueConstraint(name = "uk_track_reports_track_reporter", columnNames = { "track_id", "reporter_id" })
}, indexes = {
    @Index(name = "idx_track_reports_status_created_at", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TrackReportReason reason;

  @Column(length = 500)
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TrackReportStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
