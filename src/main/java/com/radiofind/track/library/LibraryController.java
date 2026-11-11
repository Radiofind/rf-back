package com.radiofind.track.library;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.dto.TrackResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/library")
@RequiredArgsConstructor
public class LibraryController {

  private final LibraryService libraryService;

  @GetMapping
  public PageResponse<TrackResponse> getLibrary(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) @Size(max = 150) String search,
      @RequestParam(defaultValue = "ADDED") LibrarySort sort,
      @RequestParam(defaultValue = "DESC") Sort.Direction direction,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return libraryService.getLibrary(userDetails, new LibraryQuery(search, sort, direction, page, size));
  }

  @GetMapping("/{trackId}")
  public TrackDetailsResponse getDetails(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long trackId) {
    return libraryService.getDetails(userDetails, trackId);
  }
}
