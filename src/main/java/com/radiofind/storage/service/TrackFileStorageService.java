package com.radiofind.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class TrackFileStorageService {

  private final StorageDirectory audio;

  private final StorageDirectory covers;

  public TrackFileStorageService(
      @Value("${app.storage.track-audio-path}") String audioPath,
      @Value("${app.storage.track-cover-path}") String coversPath) {
    this.audio = new StorageDirectory(audioPath, "track audio");
    this.covers = new StorageDirectory(coversPath, "track cover");
  }

  public String saveAudio(MultipartFile file, AudioType audioType) {
    return audio.save(file, audioType.getExtension());
  }

  public String saveCover(MultipartFile file, ImageType imageType) {
    return covers.save(file, imageType.getExtension());
  }

  public void deleteAudio(String fileName) {
    audio.delete(fileName);
  }

  public void deleteCover(String fileName) {
    covers.delete(fileName);
  }

  public Path getAudioPath(String fileName) {
    return audio.resolve(fileName);
  }

  public boolean coverExists(String fileName) {
    return covers.exists(fileName);
  }

  public Path getCoverPath(String fileName) {
    return covers.resolve(fileName);
  }
}
