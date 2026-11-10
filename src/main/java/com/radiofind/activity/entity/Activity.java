package com.radiofind.activity.entity;

import com.radiofind.track.entity.Track;
import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activities_user_created_at", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ActivityType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id")
  private Track track;

  @Column(name = "track_title", length = 150)
  private String trackTitle;

  @Column(name = "track_artist_name")
  private String trackArtistName;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
