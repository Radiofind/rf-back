package com.radiofind.settings.schema;

import com.radiofind.settings.entity.AccentColor;
import com.radiofind.settings.entity.AudioQuality;
import com.radiofind.settings.entity.Language;
import com.radiofind.settings.entity.ProfileVisibility;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserSettingsEnumConstraintSynchronizer {

  private static final String TABLE = "user_settings";

  private static final Map<String, Class<? extends Enum<?>>> ENUM_COLUMNS = enumColumns();

  private final JdbcTemplate jdbcTemplate;

  @SuppressWarnings("unused")
  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void synchronize() {
    ENUM_COLUMNS.forEach(this::synchronize);
  }

  private void synchronize(String column, Class<? extends Enum<?>> type) {
    String constraint = TABLE + "_" + column + "_check";
    String values = Arrays.stream(type.getEnumConstants())
        .map(value -> "'" + value.name() + "'")
        .collect(Collectors.joining(", "));

    jdbcTemplate.execute("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s, ADD CONSTRAINT %s CHECK (%s IN (%s))"
        .formatted(TABLE, constraint, constraint, column, values));
  }

  private static Map<String, Class<? extends Enum<?>>> enumColumns() {
    Map<String, Class<? extends Enum<?>>> columns = new LinkedHashMap<>();
    columns.put("language", Language.class);
    columns.put("profile_visibility", ProfileVisibility.class);
    columns.put("audio_quality", AudioQuality.class);
    columns.put("accent_color", AccentColor.class);
    return columns;
  }
}
