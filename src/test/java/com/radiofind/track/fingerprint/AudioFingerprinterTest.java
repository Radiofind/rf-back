package com.radiofind.track.fingerprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioFingerprinterTest {

  @TempDir
  Path directory;

  private Path fakeFpcalc(String script) throws IOException {
    Path fpcalc = directory.resolve("fpcalc");
    Files.writeString(fpcalc, "#!/bin/sh\n" + script + "\n");
    fpcalc.toFile().setExecutable(true);
    return fpcalc;
  }

  @Test
  void readsDurationAndRawPointsIncludingValuesAboveTheSignedRange() throws IOException {
    Path fpcalc = fakeFpcalc("echo '{\"duration\": 214.6, \"fingerprint\": [1, 4294967295, 7]}'");
    AudioFingerprinter fingerprinter = new AudioFingerprinter(fpcalc.toString(), Duration.ofSeconds(5),
        JsonMapper.builder().build());

    AudioFingerprint fingerprint = fingerprinter.fingerprint(directory.resolve("song.mp3"));

    assertEquals(215, fingerprint.durationSeconds());
    assertArrayEquals(new int[] { 1, -1, 7 }, fingerprint.points());
  }

  @Test
  void rejectsAudioTheDecoderCannotRead() throws IOException {
    Path fpcalc = fakeFpcalc("exit 2");
    AudioFingerprinter fingerprinter = new AudioFingerprinter(fpcalc.toString(), Duration.ofSeconds(5),
        JsonMapper.builder().build());

    assertThrows(UnreadableAudioException.class, () -> fingerprinter.fingerprint(directory.resolve("song.mp3")));
  }

  @Test
  void rejectsAudioWithoutAnyAnalysableSound() throws IOException {
    Path fpcalc = fakeFpcalc("echo '{\"duration\": 0, \"fingerprint\": []}'");
    AudioFingerprinter fingerprinter = new AudioFingerprinter(fpcalc.toString(), Duration.ofSeconds(5),
        JsonMapper.builder().build());

    assertThrows(UnreadableAudioException.class, () -> fingerprinter.fingerprint(directory.resolve("song.mp3")));
  }

  @Test
  void rejectsAudioThatTakesTooLongToAnalyse() throws IOException {
    Path fpcalc = fakeFpcalc("sleep 5");
    AudioFingerprinter fingerprinter = new AudioFingerprinter(fpcalc.toString(), Duration.ofMillis(200),
        JsonMapper.builder().build());

    assertThrows(UnreadableAudioException.class, () -> fingerprinter.fingerprint(directory.resolve("song.mp3")));
  }

  @Test
  void reportsAMissingFpcalcAsAnUnavailableService() {
    AudioFingerprinter fingerprinter = new AudioFingerprinter(directory.resolve("missing").toString(),
        Duration.ofSeconds(5), JsonMapper.builder().build());

    assertThrows(IllegalStateException.class, () -> fingerprinter.fingerprint(directory.resolve("song.mp3")));
  }
}
