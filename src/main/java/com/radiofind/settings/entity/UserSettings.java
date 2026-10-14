package com.radiofind.settings.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Language language = Language.EN;

  @Column(nullable = false)
  @Builder.Default
  private boolean radioMode = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ProfileVisibility profileVisibility = ProfileVisibility.EVERYONE;

  @Column(nullable = false)
  @Builder.Default
  private boolean notificationsEnabled = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AudioQuality audioQuality = AudioQuality.HIGH;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AccentColor accentColor = AccentColor.VIOLET;
}
