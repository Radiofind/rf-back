package com.radiofind.config.schema;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingsMigration {

  private static final String MIGRATION = """
      DO $$
      BEGIN
        IF EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = current_schema()
            AND table_name = 'user_settings'
            AND column_name = 'notifications_enabled'
        ) THEN
          UPDATE user_settings
            SET track_update_notifications = notifications_enabled,
                radio_update_notifications = notifications_enabled,
                like_notifications = notifications_enabled;
          ALTER TABLE user_settings DROP COLUMN notifications_enabled;
        END IF;
      END $$""";

  private final JdbcTemplate jdbcTemplate;

  @SuppressWarnings("unused")
  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void migrate() {
    jdbcTemplate.execute(MIGRATION);
  }
}
