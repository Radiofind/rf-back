package com.radiofind.storage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvatarFiles {

  private final FileStorageService fileStorageService;

  public Optional<AvatarImage> find(String avatarFileName) {
    if (avatarFileName == null) {
      return Optional.empty();
    }

    Path avatarPath = fileStorageService.getAvatarPath(avatarFileName);

    if (!Files.exists(avatarPath)) {
      return Optional.empty();
    }

    MediaType mediaType = ImageType.fromFileName(avatarFileName)
        .map(ImageType::getMediaType)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);

    return Optional.of(new AvatarImage(new FileSystemResource(avatarPath), mediaType));
  }
}
