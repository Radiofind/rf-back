package com.radiofind.track.insight;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackInsightController {

  private final TrackInsightService trackInsightService;

  @GetMapping
  public TrackDetailsResponse getDetails(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId) {
    return trackInsightService.getDetails(userDetails, trackId);
  }

  @GetMapping("/statistics")
  public TrackStatisticsResponse getStatistics(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId) {
    return trackInsightService.getStatistics(userDetails, trackId);
  }
}
