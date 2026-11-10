package com.radiofind.config.schema;

import com.radiofind.activity.entity.ActivityType;
import com.radiofind.notification.entity.NotificationType;
import com.radiofind.settings.entity.AccentColor;
import com.radiofind.settings.entity.AudioQuality;
import com.radiofind.settings.entity.Language;
import com.radiofind.settings.entity.ProfileVisibility;
import com.radiofind.storage.service.AudioType;
import com.radiofind.track.entity.TrackReviewFlagType;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.schedule.ScheduleChangeRequestStatus;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EnumCheckConstraintSynchronizer {

  private static final List<EnumColumn> ENUM_COLUMNS = List.of(
      new EnumColumn("user_settings", "language", Language.class),
      new EnumColumn("user_settings", "profile_visibility", ProfileVisibility.class),
      new EnumColumn("user_settings", "audio_quality", AudioQuality.class),
      new EnumColumn("user_settings", "accent_color", AccentColor.class),
      new EnumColumn("tracks", "status", TrackStatus.class),
      new EnumColumn("tracks", "audio_format", AudioType.class),
      new EnumColumn("activities", "type", ActivityType.class),
      new EnumColumn("notifications", "type", NotificationType.class),
      new EnumColumn("track_review_flags", "type", TrackReviewFlagType.class),
      new EnumColumn("schedule_change_requests", "status", ScheduleChangeRequestStatus.class));

  private final JdbcTemplate jdbcTemplate;

  @SuppressWarnings("unused")
  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void synchronize() {
    ENUM_COLUMNS.forEach(this::synchronize);
  }

  private void synchronize(EnumColumn enumColumn) {
    String constraint = enumColumn.table() + "_" + enumColumn.column() + "_check";
    String values = Arrays.stream(enumColumn.type().getEnumConstants())
        .map(value -> "'" + value.name() + "'")
        .collect(Collectors.joining(", "));

    jdbcTemplate.execute("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s, ADD CONSTRAINT %s CHECK (%s IN (%s))"
        .formatted(enumColumn.table(), constraint, constraint, enumColumn.column(), values));
  }

  private record EnumColumn(String table, String column, Class<? extends Enum<?>> type) {
  }
}
