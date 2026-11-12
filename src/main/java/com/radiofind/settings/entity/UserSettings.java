package com.radiofind.settings.entity;

import com.radiofind.notification.entity.NotificationCategory;
import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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

  @Column(name = "track_update_notifications", nullable = false)
  @ColumnDefault("true")
  @Builder.Default
  private boolean trackUpdateNotifications = true;

  @Column(name = "radio_update_notifications", nullable = false)
  @ColumnDefault("true")
  @Builder.Default
  private boolean radioUpdateNotifications = true;

  @Column(name = "like_notifications", nullable = false)
  @ColumnDefault("true")
  @Builder.Default
  private boolean likeNotifications = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AudioQuality audioQuality = AudioQuality.HIGH;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AccentColor accentColor = AccentColor.VIOLET;

  public boolean receives(NotificationCategory category) {
    return switch (category) {
      case TRACK_UPDATES -> trackUpdateNotifications;
      case RADIO_UPDATES -> radioUpdateNotifications;
      case LIKES -> likeNotifications;
      case MODERATION -> true;
    };
  }
}
