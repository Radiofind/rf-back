package com.radiofind.track.editing;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/tracks/{trackId}")
@RequiredArgsConstructor
public class TrackEditingController {

  private final TrackEditingService trackEditingService;

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TrackDetailsResponse edit(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId,
      @Valid @ModelAttribute EditTrackRequest request,
      @RequestPart(value = "cover", required = false) MultipartFile cover) {
    return trackEditingService.edit(userDetails, trackId, request, cover);
  }
}
