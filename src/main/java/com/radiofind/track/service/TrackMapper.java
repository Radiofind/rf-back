package com.radiofind.track.service;

import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.TrackSummaryResponse;
import com.radiofind.track.entity.Track;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackMapper {

  private static final String COVER_URL = "/api/tracks/%d/cover";

  private static final String AUDIO_URL = "/api/tracks/%d/audio?token=%s";

  public TrackResponse toResponse(Track track) {
    return toResponse(track, null, false);
  }

  public TrackResponse toResponse(Track track, String audioToken, boolean liked) {
    return TrackResponse.builder()
        .id(track.getId())
        .title(track.getTitle())
        .artistName(track.getArtistName())
        .genres(List.copyOf(track.getGenres()))
        .description(track.getDescription())
        .status(track.getStatus())
        .audioFormat(track.getAudioFormat())
        .audioSizeBytes(track.getAudioSizeBytes())
        .durationSeconds(track.getDurationSeconds())
        .coverUrl(coverUrl(track))
        .audioUrl(audioToken != null ? AUDIO_URL.formatted(track.getId(), audioToken) : null)
        .playsCount(track.getPlaysCount())
        .likesCount(track.getLikesCount())
        .liked(liked)
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
