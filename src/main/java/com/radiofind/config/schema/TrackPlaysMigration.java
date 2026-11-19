package com.radiofind.config.schema;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackPlaysMigration {

  private static final String MIGRATION = """
      INSERT INTO track_plays (track_id, listener_id, played_at)
        SELECT l.track_id, l.listener_id, l.created_at FROM track_listens l
        WHERE NOT EXISTS (
          SELECT 1 FROM track_plays p WHERE p.track_id = l.track_id AND p.listener_id = l.listener_id
        )""";

  private final JdbcTemplate jdbcTemplate;

  @SuppressWarnings("unused")
  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void migrate() {
    jdbcTemplate.execute(MIGRATION);
  }
}
