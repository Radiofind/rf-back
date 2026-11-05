package com.radiofind.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

final class StorageDirectory {

  private final Path root;

  private final String description;

  StorageDirectory(String root, String description) {
    this.root = Path.of(root).toAbsolutePath().normalize();
    this.description = description;

    try {
      Files.createDirectories(this.root);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create " + description + " storage directory", e);
    }
  }

  String save(MultipartFile file, String extension) {
    String fileName = UUID.randomUUID() + extension;
    Path targetPath = resolve(fileName);

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to save " + description + " file", e);
    }

    return fileName;
  }

  void delete(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return;
    }

    Path filePath = resolve(fileName);

    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to delete " + description + " file", e);
    }
  }

  boolean exists(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }

    return Files.isRegularFile(resolve(fileName));
  }

  Path resolve(String fileName) {
    Path filePath = root.resolve(fileName).normalize();

    if (!root.equals(filePath.getParent())) {
      throw new IllegalArgumentException("Invalid " + description + " file path");
    }

    return filePath;
  }
}
