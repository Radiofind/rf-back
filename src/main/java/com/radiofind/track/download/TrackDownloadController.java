package com.radiofind.track.download;

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
@RequestMapping("/api/tracks/{trackId}/downloads")
@RequiredArgsConstructor
public class TrackDownloadController {

  private final TrackDownloadService trackDownloadService;

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recordDownload(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long trackId) {
    trackDownloadService.recordDownload(userDetails, trackId);
  }
}
