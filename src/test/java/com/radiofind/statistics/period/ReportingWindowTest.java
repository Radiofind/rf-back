package com.radiofind.statistics.period;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingWindowTest {

  private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

  @Test
  void windowStartsAndEndsAtLocalMidnightAcrossADaylightSavingChange() {
    ReportingWindow window =
        new ReportingWindow(ZURICH, Granularity.DAY, LocalDateTime.parse("2026-10-21T00:00"), 30);

    assertEquals(Instant.parse("2026-10-20T22:00:00Z"), window.from());
    assertEquals(Instant.parse("2026-11-19T23:00:00Z"), window.to());
  }

  @Test
  void previousWindowHasTheSameNumberOfBucketsRightBefore() {
    ReportingWindow window =
        new ReportingWindow(ZURICH, Granularity.MONTH, LocalDateTime.parse("2025-12-01T00:00"), 12);

    assertEquals(Instant.parse("2024-11-30T23:00:00Z"), window.previousFrom());
  }

  @Test
  void hourlyWindowEndsAtTheStartOfTheNextHour() {
    ReportingWindow window =
        new ReportingWindow(ZURICH, Granularity.HOUR, LocalDateTime.parse("2026-11-18T12:00"), 24);

    assertEquals(Instant.parse("2026-11-18T11:00:00Z"), window.from());
    assertEquals(Instant.parse("2026-11-19T11:00:00Z"), window.to());
    assertEquals(Instant.parse("2026-11-17T11:00:00Z"), window.previousFrom());
  }

  @Test
  void everyBucketIsListedInOrder() {
    ReportingWindow window =
        new ReportingWindow(ZURICH, Granularity.WEEK, LocalDateTime.parse("2026-11-02T00:00"), 3);

    assertEquals(
        List.of(
            LocalDateTime.parse("2026-11-02T00:00"),
            LocalDateTime.parse("2026-11-09T00:00"),
            LocalDateTime.parse("2026-11-16T00:00")),
        window.bucketStarts());
  }
}
