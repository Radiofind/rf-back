package com.radiofind.track.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "track_review_flags", indexes = {
    @Index(name = "idx_track_review_flags_track", columnList = "track_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackReviewFlag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TrackReviewFlagType type;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matched_track_id", nullable = false)
  private Track matchedTrack;

  @Column(nullable = false)
  private double similarity;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
