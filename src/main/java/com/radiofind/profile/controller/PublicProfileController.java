package com.radiofind.profile.controller;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.profile.dto.PublicProfileResponse;
import com.radiofind.profile.service.ProfileFollowService;
import com.radiofind.profile.service.PublicProfileService;
import com.radiofind.profile.service.PublicTrackService;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{publicId}")
@RequiredArgsConstructor
public class PublicProfileController {

  private final PublicProfileService publicProfileService;

  private final PublicTrackService publicTrackService;

  private final ProfileFollowService profileFollowService;

  @GetMapping("/profile")
  public PublicProfileResponse getProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID publicId) {
    return publicProfileService.getProfile(userDetails, publicId);
  }

  @GetMapping("/avatar")
  public ResponseEntity<Resource> getAvatar(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID publicId) {
    return publicProfileService.getAvatar(userDetails, publicId)
        .map(avatar -> ResponseEntity.ok().contentType(avatar.mediaType()).body(avatar.resource()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/tracks")
  public PageResponse<TrackResponse> getTracks(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID publicId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return publicTrackService.getTracks(userDetails, publicId, page, size);
  }

  @PutMapping("/follow")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void follow(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID publicId) {
    profileFollowService.follow(userDetails, publicId);
  }

  @DeleteMapping("/follow")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unfollow(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID publicId) {
    profileFollowService.unfollow(userDetails, publicId);
  }
}
