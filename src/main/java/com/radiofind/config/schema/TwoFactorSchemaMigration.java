package com.radiofind.config.schema;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TwoFactorSchemaMigration {

  private static final String MIGRATION = """
      ALTER TABLE two_factor_challenges ALTER COLUMN code_hash DROP NOT NULL;
      ALTER TABLE users DROP COLUMN IF EXISTS two_factor_enabled""";

  private final JdbcTemplate jdbcTemplate;

  @SuppressWarnings("unused")
  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void migrate() {
    jdbcTemplate.execute(MIGRATION);
  }
}
