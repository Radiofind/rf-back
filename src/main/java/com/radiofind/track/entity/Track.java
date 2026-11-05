package com.radiofind.track.entity;

import com.radiofind.storage.service.AudioType;
import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tracks", indexes = {
    @Index(name = "idx_tracks_owner_created_at", columnList = "owner_id, created_at"),
    @Index(name = "idx_tracks_status_created_at", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false, length = 150)
  private String title;

  // Snapshot of the account's artist name at upload time, so later profile edits do not rewrite released tracks
  @Column(name = "artist_name", nullable = false)
  private String artistName;

  @Column(nullable = false, length = 100)
  private String genre;

  @Column(length = 100)
  private String subgenre;

  @Column(length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TrackStatus status;

  @Column(name = "audio_file_name", nullable = false)
  private String audioFileName;

  @Enumerated(EnumType.STRING)
  @Column(name = "audio_format", nullable = false, length = 20)
  private AudioType audioFormat;

  @Column(name = "audio_size_bytes", nullable = false)
  private long audioSizeBytes;

  @Column(name = "cover_file_name")
  private String coverFileName;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
