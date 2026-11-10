package com.radiofind.track.removal;

import com.radiofind.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackRemovalController {

  private final TrackRemovalService trackRemovalService;

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackRemovalService.delete(userDetails, trackId);
  }
}
