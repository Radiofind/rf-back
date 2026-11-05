package com.radiofind.storage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackFileStorageServiceTest {

  @TempDir
  Path storagePath;

  @Test
  void storesAudioUnderGeneratedNameWithExtensionOfDetectedFormat() throws IOException {
    TrackFileStorageService storage = new TrackFileStorageService(
        storagePath.resolve("audio").toString(), storagePath.resolve("covers").toString());
    byte[] content = "fLaC\0\0\0\"".getBytes();

    String fileName = storage.saveAudio(new MockMultipartFile("audio", "../song.exe", null, content), AudioType.FLAC);

    assertTrue(fileName.endsWith(".flac"));
    assertFalse(fileName.contains("song"));
    assertArrayEquals(content, Files.readAllBytes(storagePath.resolve("audio").resolve(fileName)));
  }

  @Test
  void rejectsCoverPathOutsideCoverStorage() {
    TrackFileStorageService storage = new TrackFileStorageService(
        storagePath.resolve("audio").toString(), storagePath.resolve("covers").toString());

    assertThrows(IllegalArgumentException.class, () -> storage.getCoverPath("../audio/song.flac"));
  }
}
