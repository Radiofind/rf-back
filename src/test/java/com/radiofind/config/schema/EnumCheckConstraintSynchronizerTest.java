package com.radiofind.config.schema;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EnumCheckConstraintSynchronizerTest {

  @Test
  void rebuildsTheCheckConstraintOfEveryEnumColumnFromTheCurrentEnumValues() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

    new EnumCheckConstraintSynchronizer(jdbcTemplate, mock(EntityManagerFactory.class)).synchronize();

    verify(jdbcTemplate, times(8)).execute(sql.capture());
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
                + "CHECK (accent_color IN ('VIOLET', 'OCEAN', 'ORCHID', 'PLUM'))",
            "ALTER TABLE tracks DROP CONSTRAINT IF EXISTS tracks_status_check, "
                + "ADD CONSTRAINT tracks_status_check "
                + "CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED'))",
            "ALTER TABLE tracks DROP CONSTRAINT IF EXISTS tracks_audio_format_check, "
                + "ADD CONSTRAINT tracks_audio_format_check "
                + "CHECK (audio_format IN ('MP3', 'WAV', 'FLAC'))",
            "ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_type_check, "
                + "ADD CONSTRAINT activities_type_check "
                + "CHECK (type IN ('TRACK_UPLOADED'))",
            "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check, "
                + "ADD CONSTRAINT notifications_type_check "
                + "CHECK (type IN ('TRACK_SUBMITTED_FOR_REVIEW'))"),
        sql.getAllValues());
  }
}
