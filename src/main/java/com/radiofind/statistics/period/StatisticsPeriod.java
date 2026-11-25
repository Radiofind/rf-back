package com.radiofind.statistics.period;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public enum StatisticsPeriod {
  LAST_24_HOURS(24, Granularity.HOUR),
  LAST_7_DAYS(7, Granularity.DAY),
  LAST_30_DAYS(30, Granularity.DAY),
  LAST_3_MONTHS(13, Granularity.WEEK),
  LAST_6_MONTHS(26, Granularity.WEEK),
  LAST_12_MONTHS(12, Granularity.MONTH);

  private final int buckets;

  private final Granularity granularity;

  public ReportingWindow windowAt(Instant now, ZoneId zone) {
    LocalDateTime currentBucket = granularity.bucketOf(LocalDateTime.ofInstant(now, zone));

    return new ReportingWindow(zone, granularity, currentBucket.minus(buckets - 1L, granularity.getUnit()), buckets);
  }
}
