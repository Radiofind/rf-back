package com.radiofind.track.service;

import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.TrackSummaryResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackLike;
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
    return responseOf(track, audioToken, liked)
        .own(true)
        .build();
  }

  public TrackResponse toLibraryResponse(TrackLike like, String audioToken) {
    Track track = like.getTrack();

    return responseOf(track, audioToken, true)
        .own(track.getOwner().getId().equals(like.getUser().getId()))
        .likedAt(like.getCreatedAt())
        .build();
  }

  public TrackDetailsResponse toDetails(Track track) {
    return toDetails(track, (track.getOwner().getName() + " " + track.getOwner().getSurname()).trim());
  }

  public TrackDetailsResponse toPublicDetails(Track track) {
    return toDetails(track, null);
  }

  private TrackDetailsResponse toDetails(Track track, String uploadedBy) {
    return new TrackDetailsResponse(
        track.getId(),
        track.getTitle(),
        track.getArtistName(),
        uploadedBy,
        List.copyOf(track.getGenres()),
        track.getDescription(),
        track.getStatus(),
        track.getDurationSeconds(),
        track.getAudioFormat(),
        track.getAudioSizeBytes(),
        coverUrl(track),
        track.getCreatedAt(),
        track.getUpdatedAt());
  }

  public TrackSummaryResponse toDeletedSummary(String title, String artistName) {
    return TrackSummaryResponse.builder()
        .title(title)
        .artistName(artistName)
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

  private TrackResponse.TrackResponseBuilder responseOf(Track track, String audioToken, boolean liked) {
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
        .createdAt(track.getCreatedAt());
  }

  private String coverUrl(Track track) {
    return track.getCoverFileName() != null
        ? COVER_URL.formatted(track.getId())
        : null;
  }
}
