package com.radiofind.track.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_plays", indexes = {
    @Index(name = "idx_track_plays_track_played_at", columnList = "track_id, played_at"),
    @Index(name = "idx_track_plays_listener", columnList = "listener_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackPlay {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listener_id", nullable = false)
  private User listener;

  @Column(name = "played_at", nullable = false, updatable = false)
  private Instant playedAt;
}
