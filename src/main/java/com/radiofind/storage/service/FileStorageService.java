package com.radiofind.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

  private final Path avatarsPath;

  public FileStorageService(@Value("${app.storage.avatar-path}") String avatarsPath) {
    this.avatarsPath = Path.of(avatarsPath);

    try {
      Files.createDirectories(this.avatarsPath);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create avatar storage directory", e);
    }
  }

  public String saveAvatar(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Avatar file is empty");
    }

    AvatarImageType imageType = AvatarImageType.detect(file)
        .orElseThrow(() -> new IllegalArgumentException("Avatar file is not a supported image"));

    String fileName = UUID.randomUUID() + imageType.getExtension();

    Path targetPath = avatarsPath.resolve(fileName).normalize();

    if (!targetPath.getParent().equals(avatarsPath)) {
      throw new IllegalArgumentException("Invalid avatar file path");
    }

    try {
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

      return fileName;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to save avatar file", e);
    }
  }

  public void deleteAvatar(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return;
    }

    Path filePath = avatarsPath.resolve(fileName).normalize();

    if (!filePath.getParent().equals(avatarsPath)) {
      throw new IllegalArgumentException("Invalid avatar file path");
    }

    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to delete avatar file", e);
    }
  }

  public boolean avatarExists(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }

    return Files.isRegularFile(getAvatarPath(fileName));
  }

  public Path getAvatarPath(String fileName) {
    Path filePath = avatarsPath.resolve(fileName).normalize();

    if (!filePath.getParent().equals(avatarsPath)) {
      throw new IllegalArgumentException("Invalid avatar file path");
    }

    return filePath;
  }
}
