package com.radiofind.storage.service;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum AvatarImageType {

  JPEG(MediaType.IMAGE_JPEG, ".jpg", ".jpeg"),
  PNG(MediaType.IMAGE_PNG, ".png"),
  WEBP(MediaType.parseMediaType("image/webp"), ".webp");

  private static final int HEADER_LENGTH = 12;

  private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

  private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

  private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] WEBP_FORMAT = "WEBP".getBytes(StandardCharsets.US_ASCII);

  private final MediaType mediaType;

  private final List<String> extensions;

  AvatarImageType(MediaType mediaType, String... extensions) {
    this.mediaType = mediaType;
    this.extensions = List.of(extensions);
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public String getExtension() {
    return extensions.getFirst();
  }

  public static Optional<AvatarImageType> detect(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      return detect(inputStream.readNBytes(HEADER_LENGTH));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public static Optional<AvatarImageType> fromFileName(String fileName) {
    String lowerCaseName = fileName.toLowerCase(Locale.ROOT);

    return Arrays.stream(values())
        .filter(type -> type.extensions.stream().anyMatch(lowerCaseName::endsWith))
        .findFirst();
  }

  private static Optional<AvatarImageType> detect(byte[] header) {
    if (startsWith(header, 0, JPEG_SIGNATURE)) {
      return Optional.of(JPEG);
    }

    if (startsWith(header, 0, PNG_SIGNATURE)) {
      return Optional.of(PNG);
    }

    if (startsWith(header, 0, RIFF) && startsWith(header, 8, WEBP_FORMAT)) {
      return Optional.of(WEBP);
    }

    return Optional.empty();
  }

  private static boolean startsWith(byte[] header, int offset, byte[] signature) {
    if (header.length < offset + signature.length) {
      return false;
    }

    return Arrays.equals(header, offset, offset + signature.length, signature, 0, signature.length);
  }
}
