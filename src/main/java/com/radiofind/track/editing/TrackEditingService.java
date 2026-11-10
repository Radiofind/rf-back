package com.radiofind.track.editing;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.ImageType;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.track.service.TrackInputValidator;
import com.radiofind.track.service.TrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackEditingService {

  private final OwnTracks ownTracks;

  private final TrackInputValidator trackInputValidator;

  private final TrackFileStorageService trackFileStorageService;

  private final TrackMapper trackMapper;

  private final TransactionTemplate transactionTemplate;

  private final TrackRepository trackRepository;

  public TrackDetailsResponse edit(
      CustomUserDetails userDetails,
      Long trackId,
      EditTrackRequest request,
      MultipartFile cover) {

    ImageType coverType = trackInputValidator.validateCover(cover);
    List<String> genres = trackInputValidator.validateGenres(request.getGenres());
    String newCover = coverType != null ? trackFileStorageService.saveCover(cover, coverType) : null;
    EditOutcome outcome = null;

    try {
      outcome = transactionTemplate.execute(status -> apply(userDetails, trackId, request, genres, newCover));
    } finally {
      if (outcome == null) {
        trackFileStorageService.deleteCover(newCover);
      }
    }

    if (outcome.replacedCover() != null) {
      trackFileStorageService.deleteCover(outcome.replacedCover());
    }

    return outcome.details();
  }

  private EditOutcome apply(
      CustomUserDetails userDetails,
      Long trackId,
      EditTrackRequest request,
      List<String> genres,
      String newCover) {

    Track track = ownTracks.require(userDetails, trackId);
    track.requireEditable();
    String previousCover = track.getCoverFileName();
    boolean coverChanges = newCover != null || request.isRemoveCover();

    track.setTitle(request.getTitle().trim());
    track.setGenres(new ArrayList<>(genres));
    track.setDescription(request.getDescription() == null || request.getDescription().isBlank()
        ? null
        : request.getDescription().trim());

    if (coverChanges) {
      track.setCoverFileName(newCover);
    }

    trackRepository.flush();

    return new EditOutcome(trackMapper.toDetails(track), coverChanges ? previousCover : null);
  }

  private record EditOutcome(TrackDetailsResponse details, String replacedCover) {
  }
}
