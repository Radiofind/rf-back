package com.radiofind.statistics.period;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

public record ReportingWindow(ZoneId zone, Granularity granularity, LocalDateTime firstBucket, int buckets) {

  public Instant from() {
    return startOf(firstBucket);
  }

  public Instant to() {
    return startOf(firstBucket.plus(buckets, granularity.getUnit()));
  }

  public Instant previousFrom() {
    return startOf(firstBucket.minus(buckets, granularity.getUnit()));
  }

  public List<LocalDateTime> bucketStarts() {
    return IntStream.range(0, buckets)
        .mapToObj(index -> firstBucket.plus(index, granularity.getUnit()))
        .toList();
  }

  private Instant startOf(LocalDateTime bucket) {
    return bucket.atZone(zone).toInstant();
  }
}
