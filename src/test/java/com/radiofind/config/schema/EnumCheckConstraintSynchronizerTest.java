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

    verify(jdbcTemplate, times(15)).execute(sql.capture());
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
                + "CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'))",
            "ALTER TABLE tracks DROP CONSTRAINT IF EXISTS tracks_audio_format_check, "
                + "ADD CONSTRAINT tracks_audio_format_check "
                + "CHECK (audio_format IN ('MP3', 'WAV', 'FLAC'))",
            "ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_type_check, "
                + "ADD CONSTRAINT activities_type_check "
                + "CHECK (type IN ('TRACK_UPLOADED', 'TRACK_SUBMISSION_CANCELLED', 'TRACK_RESUBMITTED', "
                + "'SCHEDULE_CHANGE_REQUESTED', 'TRACK_DELETED'))",
            "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check, "
                + "ADD CONSTRAINT notifications_type_check "
                + "CHECK (type IN ('TRACK_SUBMITTED_FOR_REVIEW', 'TRACK_SUBMISSION_CANCELLED', "
                + "'TRACK_RESUBMITTED_FOR_REVIEW', 'SCHEDULE_CHANGE_REQUESTED', 'TRACK_DELETED', 'TRACK_REPORTED', 'TRACK_LIKED'))",
            "ALTER TABLE track_review_flags DROP CONSTRAINT IF EXISTS track_review_flags_type_check, "
                + "ADD CONSTRAINT track_review_flags_type_check "
                + "CHECK (type IN ('MATCHES_OTHER_USERS_TRACK', 'RESEMBLES_OWN_TRACK', 'REUPLOAD_OF_REJECTED_TRACK'))",
            "ALTER TABLE schedule_change_requests DROP CONSTRAINT IF EXISTS schedule_change_requests_status_check, "
                + "ADD CONSTRAINT schedule_change_requests_status_check "
                + "CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED'))",
            "ALTER TABLE track_reports DROP CONSTRAINT IF EXISTS track_reports_reason_check, "
                + "ADD CONSTRAINT track_reports_reason_check "
                + "CHECK (reason IN ('COPYRIGHT_INFRINGEMENT', 'OFFENSIVE_CONTENT', 'SPAM', "
                + "'MISLEADING_INFORMATION', 'POOR_AUDIO_QUALITY', 'OTHER'))",
            "ALTER TABLE track_reports DROP CONSTRAINT IF EXISTS track_reports_status_check, "
                + "ADD CONSTRAINT track_reports_status_check "
                + "CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))",
            "ALTER TABLE users DROP CONSTRAINT IF EXISTS users_two_factor_method_check, "
                + "ADD CONSTRAINT users_two_factor_method_check "
                + "CHECK (two_factor_method IN ('EMAIL', 'RECOVERY_EMAIL', 'AUTHENTICATOR'))",
            "ALTER TABLE two_factor_challenges DROP CONSTRAINT IF EXISTS two_factor_challenges_method_check, "
                + "ADD CONSTRAINT two_factor_challenges_method_check "
                + "CHECK (method IN ('EMAIL', 'RECOVERY_EMAIL', 'AUTHENTICATOR'))",
            "ALTER TABLE two_factor_enrollments DROP CONSTRAINT IF EXISTS two_factor_enrollments_method_check, "
                + "ADD CONSTRAINT two_factor_enrollments_method_check "
                + "CHECK (method IN ('EMAIL', 'RECOVERY_EMAIL', 'AUTHENTICATOR'))"),
        sql.getAllValues());
  }
}
