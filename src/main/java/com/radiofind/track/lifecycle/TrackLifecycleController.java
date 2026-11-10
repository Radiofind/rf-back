package com.radiofind.track.lifecycle;

import com.radiofind.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackLifecycleController {

  private final TrackLifecycleService trackLifecycleService;

  @PostMapping("/cancel-submission")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelSubmission(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackLifecycleService.cancelSubmission(userDetails, trackId);
  }

  @PostMapping("/submit")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void submitForReview(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackLifecycleService.submitForReview(userDetails, trackId);
  }
}
