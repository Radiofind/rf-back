package com.radiofind.statistics.period;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingWindowTest {

  private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

  @Test
  void windowStartsAndEndsAtLocalMidnightAcrossADaylightSavingChange() {
    ReportingWindow window = new ReportingWindow(ZURICH, ChronoUnit.DAYS, LocalDate.parse("2026-10-21"), 30);

    assertEquals(Instant.parse("2026-10-20T22:00:00Z"), window.from());
    assertEquals(Instant.parse("2026-11-19T23:00:00Z"), window.to());
  }

  @Test
  void previousWindowHasTheSameNumberOfBucketsRightBefore() {
    ReportingWindow window = new ReportingWindow(ZURICH, ChronoUnit.MONTHS, LocalDate.parse("2025-12-01"), 12);

    assertEquals(Instant.parse("2024-11-30T23:00:00Z"), window.previousFrom());
  }

  @Test
  void everyBucketIsListedInOrder() {
    ReportingWindow window = new ReportingWindow(ZURICH, ChronoUnit.MONTHS, LocalDate.parse("2026-10-01"), 3);

    assertEquals(
        List.of(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01")),
        window.bucketStarts());
  }
}
