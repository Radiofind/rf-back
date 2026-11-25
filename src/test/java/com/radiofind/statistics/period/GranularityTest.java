package com.radiofind.statistics.period;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GranularityTest {

  @ParameterizedTest
  @CsvSource({
      "HOUR, 2026-11-19T11:00",
      "DAY, 2026-11-19T00:00",
      "WEEK, 2026-11-16T00:00",
      "MONTH, 2026-11-01T00:00"
  })
  void momentBelongsToTheBucketStartingAtItsHourDayMondayOrFirstOfMonth(
      Granularity granularity, LocalDateTime bucket) {
    LocalDateTime start = granularity.bucketOf(LocalDateTime.parse("2026-11-19T11:45:30"));

    assertEquals(bucket, start);
  }

  @ParameterizedTest
  @CsvSource({
      "2026-11-16T00:00, 2026-11-16T00:00",
      "2026-11-22T23:59, 2026-11-16T00:00"
  })
  void weekRunsFromMondayToSunday(LocalDateTime moment, LocalDateTime monday) {
    LocalDateTime start = Granularity.WEEK.bucketOf(moment);

    assertEquals(monday, start);
  }
}
