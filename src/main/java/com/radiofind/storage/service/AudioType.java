package com.radiofind.storage.service;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public enum AudioType {

  MP3(MediaType.parseMediaType("audio/mpeg"), ".mp3"),
  WAV(MediaType.parseMediaType("audio/wav"), ".wav"),
  FLAC(MediaType.parseMediaType("audio/flac"), ".flac");

  private static final int HEADER_LENGTH = 12;

  private static final byte[] ID3_TAG = "ID3".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] WAVE_FORMAT = "WAVE".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] FLAC_MARKER = "fLaC".getBytes(StandardCharsets.US_ASCII);

  private final MediaType mediaType;

  private final String extension;

  AudioType(MediaType mediaType, String extension) {
    this.mediaType = mediaType;
    this.extension = extension;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public String getExtension() {
    return extension;
  }

  public static Optional<AudioType> detect(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      return detect(inputStream.readNBytes(HEADER_LENGTH));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static Optional<AudioType> detect(byte[] header) {
    if (startsWith(header, 0, ID3_TAG) || isMpegAudioFrame(header)) {
      return Optional.of(MP3);
    }

    if (startsWith(header, 0, RIFF) && startsWith(header, 8, WAVE_FORMAT)) {
      return Optional.of(WAV);
    }

    if (startsWith(header, 0, FLAC_MARKER)) {
      return Optional.of(FLAC);
    }

    return Optional.empty();
  }

  // 11 set sync bits followed by a non-reserved layer: an MPEG audio frame without an ID3 tag
  private static boolean isMpegAudioFrame(byte[] header) {
    if (header.length < 2) {
      return false;
    }

    int second = header[1] & 0xFF;
    int layer = (second >> 1) & 0b11;

    return (header[0] & 0xFF) == 0xFF && (second & 0xE0) == 0xE0 && layer != 0;
  }

  private static boolean startsWith(byte[] header, int offset, byte[] signature) {
    if (header.length < offset + signature.length) {
      return false;
    }

    return Arrays.equals(header, offset, offset + signature.length, signature, 0, signature.length);
  }
}
