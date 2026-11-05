package com.radiofind.config.schema;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackGenresMigration {

  private static final String MIGRATION = """
      DO $$
      BEGIN
        IF EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = current_schema() AND table_name = 'tracks' AND column_name = 'genre'
        ) THEN
          INSERT INTO track_genres (track_id, position, genre)
            SELECT id, 0, genre FROM tracks WHERE genre IS NOT NULL;
          INSERT INTO track_genres (track_id, position, genre)
            SELECT id, 1, subgenre FROM tracks WHERE subgenre IS NOT NULL;
          ALTER TABLE tracks DROP COLUMN genre, DROP COLUMN IF EXISTS subgenre;
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
