package com.radiofind.track.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.UploadTrackRequest;
import com.radiofind.track.service.TrackCover;
import com.radiofind.track.service.TrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

  private final TrackService trackService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public TrackResponse uploadTrack(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @ModelAttribute UploadTrackRequest request,
      @RequestPart("audio") MultipartFile audio,
      @RequestPart(value = "cover", required = false) MultipartFile cover) {
    return trackService.upload(userDetails, request, audio, cover);
  }

  @GetMapping("/{trackId}/cover")
  public ResponseEntity<Resource> getCover(@PathVariable Long trackId) {
    TrackCover cover = trackService.getCover(trackId);

    return ResponseEntity.ok()
        .contentType(cover.mediaType())
        .body(new FileSystemResource(cover.path()));
  }
}
