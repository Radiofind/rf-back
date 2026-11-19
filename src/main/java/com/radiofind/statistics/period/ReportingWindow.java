package com.radiofind.statistics.period;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

public record ReportingWindow(ZoneId zone, ChronoUnit unit, LocalDate firstBucket, int buckets) {

  public Instant from() {
    return startOf(firstBucket);
  }

  public Instant to() {
    return startOf(firstBucket.plus(buckets, unit));
  }

  public Instant previousFrom() {
    return startOf(firstBucket.minus(buckets, unit));
  }

  public List<LocalDate> bucketStarts() {
    return IntStream.range(0, buckets)
        .mapToObj(index -> firstBucket.plus(index, unit))
        .toList();
  }

  private Instant startOf(LocalDate day) {
    return day.atStartOfDay(zone).toInstant();
  }
}
