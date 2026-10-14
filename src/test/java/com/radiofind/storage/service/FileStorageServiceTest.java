package com.radiofind.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

  @TempDir
  Path avatarsPath;

  private FileStorageService fileStorageService;

  @BeforeEach
  void setUp() {
    fileStorageService = new FileStorageService(avatarsPath.toString());
  }

  @Test
  void avatarExistsWhenFileIsStored() throws IOException {
    Files.writeString(avatarsPath.resolve("avatar.png"), "image");

    assertTrue(fileStorageService.avatarExists("avatar.png"));
  }

  @Test
  void avatarDoesNotExistWhenFileIsMissing() {
    assertFalse(fileStorageService.avatarExists("missing.png"));
  }

  @Test
  void avatarDoesNotExistWithoutFileName() {
    assertFalse(fileStorageService.avatarExists(null));
    assertFalse(fileStorageService.avatarExists(" "));
  }

  @Test
  void directoryIsNotTreatedAsAvatar() throws IOException {
    Files.createDirectory(avatarsPath.resolve("folder.png"));

    assertFalse(fileStorageService.avatarExists("folder.png"));
  }

  @Test
  void rejectsPathOutsideAvatarStorage() {
    assertThrows(IllegalArgumentException.class,
        () -> fileStorageService.avatarExists("../secret.png"));
  }
}
