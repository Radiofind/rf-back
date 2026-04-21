package com.radiofind.track.controller;

import com.radiofind.track.entity.Track;
import com.radiofind.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

  private final TrackService trackService;

  @PostMapping("/upload")
  public Track upload(@RequestParam("file") MultipartFile file, @RequestParam("title") String title) {
    return trackService.upload(file, title);
  }
}
