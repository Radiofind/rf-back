package com.radiofind.track.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.common.dto.PageResponse;
import com.radiofind.genre.service.GenreCatalog;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.AudioType;
import com.radiofind.storage.service.ImageType;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.dto.OwnTracksQuery;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.UploadTrackRequest;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.entity.TrackFingerprint;
import com.radiofind.track.entity.TrackReviewFlag;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.fingerprint.AudioFingerprint;
import com.radiofind.track.fingerprint.AudioFingerprinter;
import com.radiofind.track.fingerprint.DuplicateCheck;
import com.radiofind.track.fingerprint.DuplicateTrackDetector;
import com.radiofind.track.fingerprint.UnreadableAudioException;
import com.radiofind.track.repository.TrackFingerprintRepository;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.repository.TrackReviewFlagRepository;
import com.radiofind.track.repository.TrackSpecifications;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class TrackService {

  private static final long MAX_AUDIO_SIZE = 100L * 1024 * 1024;

  private static final long MAX_COVER_SIZE = 10L * 1024 * 1024;

  private final TrackRepository trackRepository;

  private final UserRepository userRepository;

  private final TrackFileStorageService trackFileStorageService;

  private final GenreCatalog genreCatalog;

  private final TrackMapper trackMapper;

  private final ApplicationEventPublisher eventPublisher;

  private final TransactionTemplate transactionTemplate;

  private final AudioFingerprinter audioFingerprinter;

  private final DuplicateTrackDetector duplicateTrackDetector;

  private final TrackFingerprintRepository trackFingerprintRepository;

  private final TrackReviewFlagRepository trackReviewFlagRepository;

  private final AudioStreamTokens audioStreamTokens;

  private final TrackLikeRepository trackLikeRepository;

  public TrackResponse upload(
      CustomUserDetails userDetails,
      UploadTrackRequest request,
      MultipartFile audio,
      MultipartFile cover) {

    AudioType audioType = validateAudio(audio);
    ImageType coverType = validateCover(cover);
    List<String> genres = request.getGenres().stream().map(String::trim).toList();
    validateGenres(genres);

    String audioFileName = trackFileStorageService.saveAudio(audio, audioType);
    String coverFileName = null;
    Track track = null;

    try {
      coverFileName = coverType != null
          ? trackFileStorageService.saveCover(cover, coverType)
          : null;

      AudioFingerprint fingerprint = fingerprint(audioFileName);

      Track draft = Track.builder()
          .title(request.getTitle().trim())
          .genres(new ArrayList<>(genres))
          .description(normalizeNullableText(request.getDescription()))
          .status(TrackStatus.PENDING_REVIEW)
          .audioFileName(audioFileName)
          .audioFormat(audioType)
          .audioSizeBytes(audio.getSize())
          .coverFileName(coverFileName)
          .durationSeconds(fingerprint.durationSeconds())
          .build();

      track = transactionTemplate.execute(
          status -> persist(userDetails.getUser().getId(), draft, fingerprint));
    } finally {
      if (track == null) {
        trackFileStorageService.deleteAudio(audioFileName);
        trackFileStorageService.deleteCover(coverFileName);
      }
    }

    return trackMapper.toResponse(track);
  }

  @Transactional(readOnly = true)
  public PageResponse<TrackResponse> getOwnTracks(CustomUserDetails userDetails, OwnTracksQuery query) {
    Long ownerId = userDetails.getUser().getId();
    Specification<Track> filter = Specification.allOf(
        TrackSpecifications.ownedBy(ownerId),
        TrackSpecifications.inStatus(query.status()),
        TrackSpecifications.titleOrArtistContains(query.search()));
    Page<Track> tracks = trackRepository.findAll(
        filter,
        PageRequest.of(query.page(), query.size(), query.sort().toSort(query.direction())));
    Set<Long> likedTrackIds = tracks.isEmpty()
        ? Set.of()
        : trackLikeRepository.findLikedTrackIds(ownerId, tracks.map(Track::getId).toList());

    return PageResponse.of(tracks, track -> trackMapper.toResponse(
        track,
        audioStreamTokens.issue(track.getId()),
        likedTrackIds.contains(track.getId())));
  }

  @Transactional(readOnly = true)
  public TrackAudio getAudio(Long trackId, String token) {
    if (!audioStreamTokens.grants(token, trackId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Audio link is invalid or expired");
    }

    Track track = trackRepository.findById(trackId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));

    return new TrackAudio(
        trackFileStorageService.getAudioPath(track.getAudioFileName()),
        track.getAudioFormat().getMediaType());
  }

  @Transactional(readOnly = true)
  public TrackCover getCover(Long trackId) {
    Track track = trackRepository.findById(trackId)
        .filter(candidate -> trackFileStorageService.coverExists(candidate.getCoverFileName()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cover not found"));

    MediaType mediaType = ImageType.fromFileName(track.getCoverFileName())
        .map(ImageType::getMediaType)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);

    return new TrackCover(trackFileStorageService.getCoverPath(track.getCoverFileName()), mediaType);
  }

  private AudioFingerprint fingerprint(String audioFileName) {
    try {
      return audioFingerprinter.fingerprint(trackFileStorageService.getAudioPath(audioFileName));
    } catch (UnreadableAudioException e) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), e);
    }
  }

  private Track persist(Long ownerId, Track draft, AudioFingerprint fingerprint) {
    User owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    trackRepository.lockUploadsOf(ownerId);
    DuplicateCheck duplicateCheck = duplicateTrackDetector.check(ownerId, fingerprint);

    if (duplicateCheck.ownDuplicate().isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already uploaded this track");
    }

    draft.setOwner(owner);
    draft.setArtistName(resolveArtistName(owner));

    Track track = trackRepository.save(draft);
    trackFingerprintRepository.save(TrackFingerprint.builder()
        .track(track)
        .points(fingerprint.points())
        .build());
    trackReviewFlagRepository.saveAll(duplicateCheck.flaggedMatches().stream()
        .map(match -> TrackReviewFlag.builder()
            .track(track)
            .type(match.flag())
            .matchedTrack(match.track())
            .similarity(match.similarity())
            .build())
        .toList());
    eventPublisher.publishEvent(new TrackUploadedEvent(track));

    return track;
  }

  private String resolveArtistName(User owner) {
    ArtistProfile artistProfile = owner.getArtistProfile();
    String artistName = artistProfile != null
        ? normalizeNullableText(artistProfile.getArtistName())
        : null;

    return artistName != null
        ? artistName
        : (owner.getName() + " " + owner.getSurname()).trim();
  }

  private AudioType validateAudio(MultipartFile audio) {
    if (audio == null || audio.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is empty");
    }

    if (audio.getSize() > MAX_AUDIO_SIZE) {
      throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Audio file must not exceed 100 MB");
    }

    return AudioType.detect(audio)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Only MP3, WAV and FLAC audio files are allowed"));
  }

  private ImageType validateCover(MultipartFile cover) {
    if (cover == null || cover.isEmpty()) {
      return null;
    }

    if (cover.getSize() > MAX_COVER_SIZE) {
      throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Cover image must not exceed 10 MB");
    }

    return ImageType.detect(cover)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Only JPEG, PNG and WebP cover images are allowed"));
  }

  private void validateGenres(List<String> genres) {
    if (!genres.stream().allMatch(genreCatalog::isGenre)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown genre");
    }

    if (new HashSet<>(genres).size() != genres.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genres must not repeat");
    }
  }

  private String normalizeNullableText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }
}
