package com.radiofind.track.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.genre.service.GenreCatalog;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.AudioType;
import com.radiofind.storage.service.ImageType;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.UploadTrackRequest;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


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

  public TrackResponse upload(
      CustomUserDetails userDetails,
      UploadTrackRequest request,
      MultipartFile audio,
      MultipartFile cover) {

    AudioType audioType = validateAudio(audio);
    ImageType coverType = validateCover(cover);
    String genre = request.getGenre().trim();
    String subgenre = normalizeNullableText(request.getSubgenre());
    validateGenre(genre, subgenre);

    String audioFileName = trackFileStorageService.saveAudio(audio, audioType);
    String coverFileName = null;
    Track track = null;

    try {
      coverFileName = coverType != null
          ? trackFileStorageService.saveCover(cover, coverType)
          : null;

      Track draft = Track.builder()
          .title(request.getTitle().trim())
          .genre(genre)
          .subgenre(subgenre)
          .description(normalizeNullableText(request.getDescription()))
          .status(TrackStatus.PENDING_REVIEW)
          .audioFileName(audioFileName)
          .audioFormat(audioType)
          .audioSizeBytes(audio.getSize())
          .coverFileName(coverFileName)
          .build();

      track = transactionTemplate.execute(status -> persist(userDetails.getUser().getId(), draft));
    } finally {
      if (track == null) {
        trackFileStorageService.deleteAudio(audioFileName);
        trackFileStorageService.deleteCover(coverFileName);
      }
    }

    return trackMapper.toResponse(track);
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

  private Track persist(Long ownerId, Track draft) {
    User owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    draft.setOwner(owner);
    draft.setArtistName(resolveArtistName(owner));

    Track track = trackRepository.save(draft);
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

  private void validateGenre(String genre, String subgenre) {
    if (!genreCatalog.isGenre(genre)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown genre");
    }

    if (subgenre == null) {
      return;
    }

    if (!genreCatalog.isGenre(subgenre)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown subgenre");
    }

    if (subgenre.equals(genre)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subgenre must differ from the genre");
    }
  }

  private String normalizeNullableText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }
}
