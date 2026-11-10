package com.radiofind.track.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_listens", uniqueConstraints = {
    @UniqueConstraint(name = "uk_track_listens_track_listener", columnNames = { "track_id", "listener_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackListen {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listener_id", nullable = false)
  private User listener;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
