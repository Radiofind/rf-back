package com.radiofind.track.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OwnTracks {

  private final TrackRepository trackRepository;

  public Track require(CustomUserDetails userDetails, Long trackId) {
    return trackRepository.findByIdAndOwnerId(trackId, userDetails.getUser().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
  }
}
