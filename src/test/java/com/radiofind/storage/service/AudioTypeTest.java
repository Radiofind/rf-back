package com.radiofind.storage.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioTypeTest {

  static Stream<Arguments> audioHeaders() {
    return Stream.of(
        Arguments.of("ID3\u0004\0\0\0\0\0\0", Optional.of(AudioType.MP3)),
        Arguments.of("ÿû\u0090d", Optional.of(AudioType.MP3)),
        Arguments.of("RIFF\0\0\0\0WAVEfmt ", Optional.of(AudioType.WAV)),
        Arguments.of("fLaC\0\0\0\"", Optional.of(AudioType.FLAC)),
        Arguments.of("RIFF\0\0\0\0WEBPVP8 ", Optional.empty()),
        Arguments.of("ÿùP\u0080", Optional.empty()),
        Arguments.of("<html></html>", Optional.empty()),
        Arguments.of("ÿ", Optional.empty()));
  }

  @ParameterizedTest
  @MethodSource("audioHeaders")
  void detectsAudioFormatByContentNotByName(String header, Optional<AudioType> expected) {
    MockMultipartFile file = new MockMultipartFile(
        "audio", "song.mp3", "audio/mpeg", header.getBytes(StandardCharsets.ISO_8859_1));

    assertEquals(expected, AudioType.detect(file));
  }
}
