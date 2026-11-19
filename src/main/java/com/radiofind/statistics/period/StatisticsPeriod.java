package com.radiofind.statistics.period;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public enum StatisticsPeriod {
  LAST_7_DAYS(7, ChronoUnit.DAYS),
  LAST_30_DAYS(30, ChronoUnit.DAYS),
  LAST_12_MONTHS(12, ChronoUnit.MONTHS);

  private final int buckets;

  private final ChronoUnit unit;

  public ReportingWindow windowAt(Instant now, ZoneId zone) {
    LocalDate today = LocalDate.ofInstant(now, zone);
    LocalDate currentBucket = unit == ChronoUnit.MONTHS ? today.withDayOfMonth(1) : today;

    return new ReportingWindow(zone, unit, currentBucket.minus(buckets - 1L, unit), buckets);
  }
}
