package com.radiofind.track.controller;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.service.TrackService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tracks")
@RequiredArgsConstructor
public class UserTrackController {

  private final TrackService trackService;

  @GetMapping
  public PageResponse<TrackResponse> getOwnTracks(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) TrackStatus status,
      @RequestParam(required = false) @Size(max = 150) String search,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return trackService.getOwnTracks(userDetails, status, search, page, size);
  }
}
