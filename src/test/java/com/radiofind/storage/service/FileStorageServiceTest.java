package com.radiofind.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @Test
  void savesAvatarWithExtensionDetectedFromContent() throws IOException {
    String fileName = fileStorageService.saveAvatar(
        new MockMultipartFile("file", "photo.html", "text/html", TestImages.JPEG));

    assertTrue(fileName.endsWith(".jpg"));
    assertArrayEquals(TestImages.JPEG, Files.readAllBytes(avatarsPath.resolve(fileName)));
  }

  @Test
  void rejectsAvatarWithUnsupportedContent() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveAvatar(
        new MockMultipartFile("file", "avatar.png", "image/png", TestImages.HTML)));

    try (var files = Files.list(avatarsPath)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void rejectsEmptyAvatar() {
    assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveAvatar(
        new MockMultipartFile("file", "avatar.png", "image/png", new byte[0])));
  }

  @Test
  void deletesStoredAvatar() throws IOException {
    Files.writeString(avatarsPath.resolve("avatar.png"), "image");

    fileStorageService.deleteAvatar("avatar.png");

    assertFalse(Files.exists(avatarsPath.resolve("avatar.png")));
  }

  @Test
  void deleteIgnoresMissingAvatarAndBlankName() {
    assertDoesNotThrow(() -> fileStorageService.deleteAvatar("missing.png"));
    assertDoesNotThrow(() -> fileStorageService.deleteAvatar(null));
    assertDoesNotThrow(() -> fileStorageService.deleteAvatar(" "));
  }

  @Test
  void deleteRejectsPathOutsideAvatarStorage() {
    assertThrows(IllegalArgumentException.class,
        () -> fileStorageService.deleteAvatar("../secret.png"));
  }
}
