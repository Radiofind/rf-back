package com.radiofind.track.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  private String fileUrl;

  @Enumerated(EnumType.STRING)
  private TrackStatus status;

  private LocalDateTime uploadedAt;

  private LocalDateTime scheduledAt;

  @ManyToOne
  private User user;
}
