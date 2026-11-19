package com.radiofind.statistics;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.statistics.period.StatisticsPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

@RestController
@RequestMapping("/api/users/me/statistics")
@RequiredArgsConstructor
public class StatisticsController {

  private final StatisticsService statisticsService;

  @GetMapping
  public StatisticsResponse getStatistics(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "LAST_30_DAYS") StatisticsPeriod period,
      @RequestParam(defaultValue = "UTC") ZoneId zone) {
    return statisticsService.getStatistics(userDetails, period, zone);
  }
}
