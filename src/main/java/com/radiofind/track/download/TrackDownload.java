package com.radiofind.track.download;

import com.radiofind.track.entity.Track;
import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_downloads", indexes = {
    @Index(name = "idx_track_downloads_track_downloaded_at", columnList = "track_id, downloaded_at"),
    @Index(name = "idx_track_downloads_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackDownload {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "downloaded_at", nullable = false, updatable = false)
  private Instant downloadedAt;
}
