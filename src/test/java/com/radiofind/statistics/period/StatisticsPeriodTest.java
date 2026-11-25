package com.radiofind.statistics.period;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsPeriodTest {

  private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

  @ParameterizedTest
  @CsvSource({
      "LAST_24_HOURS, HOUR, 2026-11-18T12:00, 24",
      "LAST_7_DAYS, DAY, 2026-11-13T00:00, 7",
      "LAST_30_DAYS, DAY, 2026-10-21T00:00, 30",
      "LAST_3_MONTHS, WEEK, 2026-08-24T00:00, 13",
      "LAST_6_MONTHS, WEEK, 2026-05-25T00:00, 26",
      "LAST_12_MONTHS, MONTH, 2025-12-01T00:00, 12"
  })
  void windowEndsWithTheCurrentBucket(
      StatisticsPeriod period, Granularity granularity, LocalDateTime firstBucket, int buckets) {
    ReportingWindow window = period.windowAt(Instant.parse("2026-11-19T10:30:00Z"), ZURICH);

    assertEquals(new ReportingWindow(ZURICH, granularity, firstBucket, buckets), window);
  }

  @Test
  void todayIsTheViewersLocalDate() {
    ReportingWindow window = StatisticsPeriod.LAST_7_DAYS.windowAt(Instant.parse("2026-11-19T23:30:00Z"), ZURICH);

    assertEquals(LocalDateTime.parse("2026-11-14T00:00"), window.firstBucket());
  }
}
