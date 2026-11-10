package com.radiofind.track.service;

import com.radiofind.genre.service.GenreCatalog;
import com.radiofind.storage.service.AudioType;
import com.radiofind.storage.service.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackInputValidator {

  private static final long MAX_AUDIO_SIZE = 100L * 1024 * 1024;

  private static final long MAX_COVER_SIZE = 10L * 1024 * 1024;

  private final GenreCatalog genreCatalog;

  public AudioType validateAudio(MultipartFile audio) {
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

  public ImageType validateCover(MultipartFile cover) {
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

  public List<String> validateGenres(List<String> requested) {
    List<String> genres = requested.stream().map(String::trim).toList();

    if (!genres.stream().allMatch(genreCatalog::isGenre)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown genre");
    }

    if (new HashSet<>(genres).size() != genres.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genres must not repeat");
    }

    return genres;
  }
}
