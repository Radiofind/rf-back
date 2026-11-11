package com.radiofind.track.report;

import com.radiofind.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracks/{trackId}/reports")
@RequiredArgsConstructor
public class TrackReportController {

  private final TrackReportService trackReportService;

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void report(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId,
      @Valid @RequestBody TrackReportRequest request) {
    trackReportService.report(userDetails, trackId, request);
  }
}
