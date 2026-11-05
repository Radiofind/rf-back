package com.radiofind.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class FileStorageService {

  private final StorageDirectory avatars;

  public FileStorageService(@Value("${app.storage.avatar-path}") String avatarsPath) {
    this.avatars = new StorageDirectory(avatarsPath, "avatar");
  }

  public String saveAvatar(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Avatar file is empty");
    }

    ImageType imageType = ImageType.detect(file)
        .orElseThrow(() -> new IllegalArgumentException("Avatar file is not a supported image"));

    return avatars.save(file, imageType.getExtension());
  }

  public void deleteAvatar(String fileName) {
    avatars.delete(fileName);
  }

  public boolean avatarExists(String fileName) {
    return avatars.exists(fileName);
  }

  public Path getAvatarPath(String fileName) {
    return avatars.resolve(fileName);
  }
}
