package com.radiofind.track.library;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.TrackLike;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.track.service.AudioStreamTokens;
import com.radiofind.track.service.TrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LibraryService {

  private final TrackLikeRepository trackLikeRepository;

  private final TrackMapper trackMapper;

  private final AudioStreamTokens audioStreamTokens;

  @Transactional(readOnly = true)
  public PageResponse<TrackResponse> getLibrary(CustomUserDetails userDetails, LibraryQuery query) {
    Specification<TrackLike> filter = Specification.allOf(
        LibrarySpecifications.likedBy(userDetails.getUser().getId()),
        LibrarySpecifications.titleOrArtistContains(query.search()));

    return PageResponse.of(
        trackLikeRepository.findAll(
            filter,
            PageRequest.of(query.page(), query.size(), query.sort().toSort(query.direction()))),
        like -> trackMapper.toLibraryResponse(like, audioStreamTokens.issue(like.getTrack().getId())));
  }

  @Transactional(readOnly = true)
  public TrackDetailsResponse getDetails(CustomUserDetails userDetails, Long trackId) {
    return trackLikeRepository.findLikedTrack(trackId, userDetails.getUser().getId())
        .map(trackMapper::toPublicDetails)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track is not in your library"));
  }
}
