package com.radiofind.statistics.period;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsPeriodTest {

  private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

  @ParameterizedTest
  @CsvSource({
      "LAST_7_DAYS, DAYS, 2026-11-13, 7",
      "LAST_30_DAYS, DAYS, 2026-10-21, 30",
      "LAST_12_MONTHS, MONTHS, 2025-12-01, 12"
  })
  void windowEndsWithTheCurrentBucket(StatisticsPeriod period, ChronoUnit unit, LocalDate firstBucket, int buckets) {
    ReportingWindow window = period.windowAt(Instant.parse("2026-11-19T10:00:00Z"), ZURICH);

    assertEquals(new ReportingWindow(ZURICH, unit, firstBucket, buckets), window);
  }

  @Test
  void todayIsTheViewersLocalDate() {
    ReportingWindow window = StatisticsPeriod.LAST_7_DAYS.windowAt(Instant.parse("2026-11-19T23:30:00Z"), ZURICH);

    assertEquals(LocalDate.parse("2026-11-14"), window.firstBucket());
  }
}
