package com.radiofind.track.review;

import com.radiofind.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackReviewController {

  private final TrackReviewService trackReviewService;

  @GetMapping("/rejection")
  public RejectionReasonResponse getRejectionReason(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId) {
    return trackReviewService.getRejectionReason(userDetails, trackId);
  }
}
