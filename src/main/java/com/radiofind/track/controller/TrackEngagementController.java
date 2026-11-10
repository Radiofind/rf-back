package com.radiofind.track.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.service.TrackEngagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackEngagementController {

  private final TrackEngagementService trackEngagementService;

  @PostMapping("/plays")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recordPlay(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackEngagementService.recordPlay(userDetails, trackId);
  }

  @PutMapping("/like")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void like(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackEngagementService.like(userDetails, trackId);
  }

  @DeleteMapping("/like")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unlike(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackEngagementService.unlike(userDetails, trackId);
  }
}
