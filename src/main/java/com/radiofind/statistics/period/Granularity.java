package com.radiofind.statistics.period;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Getter
@RequiredArgsConstructor
public enum Granularity {
  HOUR(ChronoUnit.HOURS, "hour"),
  DAY(ChronoUnit.DAYS, "day"),
  WEEK(ChronoUnit.WEEKS, "week"),
  MONTH(ChronoUnit.MONTHS, "month");

  private final ChronoUnit unit;

  private final String truncation;

  public LocalDateTime bucketOf(LocalDateTime time) {
    return switch (this) {
      case HOUR -> time.truncatedTo(ChronoUnit.HOURS);
      case DAY -> time.truncatedTo(ChronoUnit.DAYS);
      case WEEK -> time.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
    };
  }
}
