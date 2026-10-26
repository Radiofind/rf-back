package com.radiofind.settings.schema;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UserSettingsEnumConstraintSynchronizerTest {

  @Test
  void rebuildsTheCheckConstraintOfEveryEnumColumnFromTheCurrentEnumValues() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

    new UserSettingsEnumConstraintSynchronizer(jdbcTemplate, mock(EntityManagerFactory.class)).synchronize();

    verify(jdbcTemplate, times(4)).execute(sql.capture());
    assertEquals(
        List.of(
            "ALTER TABLE user_settings DROP CONSTRAINT IF EXISTS user_settings_language_check, "
                + "ADD CONSTRAINT user_settings_language_check "
                + "CHECK (language IN ('EN', 'DE', 'FR', 'IT', 'ES', 'RU'))",
            "ALTER TABLE user_settings DROP CONSTRAINT IF EXISTS user_settings_profile_visibility_check, "
                + "ADD CONSTRAINT user_settings_profile_visibility_check "
                + "CHECK (profile_visibility IN ('EVERYONE', 'FOLLOWERS', 'ONLY_ME'))",
            "ALTER TABLE user_settings DROP CONSTRAINT IF EXISTS user_settings_audio_quality_check, "
                + "ADD CONSTRAINT user_settings_audio_quality_check "
                + "CHECK (audio_quality IN ('LOW', 'NORMAL', 'HIGH'))",
            "ALTER TABLE user_settings DROP CONSTRAINT IF EXISTS user_settings_accent_color_check, "
                + "ADD CONSTRAINT user_settings_accent_color_check "
                + "CHECK (accent_color IN ('VIOLET', 'OCEAN', 'ORCHID', 'PLUM'))"),
        sql.getAllValues());
  }
}
