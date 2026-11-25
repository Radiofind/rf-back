package com.radiofind.track.service;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrackListing {

  private final TrackLikeRepository trackLikeRepository;

  private final TrackMapper trackMapper;

  private final AudioStreamTokens audioStreamTokens;

  public PageResponse<TrackResponse> forListener(Page<Track> tracks, long listenerId) {
    Set<Long> likedTrackIds = tracks.isEmpty()
        ? Set.of()
        : trackLikeRepository.findLikedTrackIds(listenerId, tracks.map(Track::getId).toList());

    return PageResponse.of(tracks, track -> trackMapper.toListenerResponse(
        track,
        audioStreamTokens.issue(track.getId()),
        likedTrackIds.contains(track.getId()),
        listenerId));
  }
}
