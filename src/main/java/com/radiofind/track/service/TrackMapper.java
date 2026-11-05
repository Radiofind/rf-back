package com.radiofind.track.service;

import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.TrackSummaryResponse;
import com.radiofind.track.entity.Track;
import org.springframework.stereotype.Component;

@Component
public class TrackMapper {

  private static final String COVER_URL = "/api/tracks/%d/cover";

  public TrackResponse toResponse(Track track) {
    return TrackResponse.builder()
        .id(track.getId())
        .title(track.getTitle())
        .artistName(track.getArtistName())
        .genre(track.getGenre())
        .subgenre(track.getSubgenre())
        .description(track.getDescription())
        .status(track.getStatus())
        .audioFormat(track.getAudioFormat())
        .audioSizeBytes(track.getAudioSizeBytes())
        .coverUrl(coverUrl(track))
        .createdAt(track.getCreatedAt())
        .build();
  }

  public TrackSummaryResponse toSummary(Track track) {
    return TrackSummaryResponse.builder()
        .id(track.getId())
        .title(track.getTitle())
        .artistName(track.getArtistName())
        .coverUrl(coverUrl(track))
        .status(track.getStatus())
        .build();
  }

  private String coverUrl(Track track) {
    return track.getCoverFileName() != null
        ? COVER_URL.formatted(track.getId())
        : null;
  }
}
