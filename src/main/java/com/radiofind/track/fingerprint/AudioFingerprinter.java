package com.radiofind.track.fingerprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AudioFingerprinter {

  private final String fpcalcPath;

  private final Duration timeout;

  private final ObjectMapper objectMapper;

  public AudioFingerprinter(
      @Value("${app.fingerprint.fpcalc-path}") String fpcalcPath,
      @Value("${app.fingerprint.timeout}") Duration timeout,
      ObjectMapper objectMapper) {
    this.fpcalcPath = fpcalcPath;
    this.timeout = timeout;
    this.objectMapper = objectMapper;
  }

  public AudioFingerprint fingerprint(Path audio) {
    Process process = start(audio);
    byte[] output = awaitOutput(process, audio);

    if (process.exitValue() != 0) {
      throw new UnreadableAudioException("Audio file could not be decoded");
    }

    return parse(output);
  }

  private Process start(Path audio) {
    try {
      return new ProcessBuilder(fpcalcPath, "-raw", "-json", audio.toString())
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
    } catch (IOException e) {
      log.atError()
          .setMessage("fpcalc could not be started, track uploads are impossible")
          .addKeyValue("fpcalcPath", fpcalcPath)
          .addKeyValue("fix", "install Chromaprint (fpcalc) or set FPCALC_PATH")
          .setCause(e)
          .log();
      throw new IllegalStateException("Audio fingerprinting is unavailable", e);
    }
  }

  private byte[] awaitOutput(Process process, Path audio) {
    try {
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        log.atWarn()
            .setMessage("fpcalc timed out, upload rejected")
            .addKeyValue("file", audio.getFileName())
            .addKeyValue("timeout", timeout)
            .log();
        throw new UnreadableAudioException("Audio file could not be analysed in time");
      }

      try (InputStream stdout = process.getInputStream()) {
        return stdout.readAllBytes();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read fpcalc output", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new IllegalStateException("Interrupted while fingerprinting audio", e);
    }
  }

  private AudioFingerprint parse(byte[] output) {
    JsonNode json = objectMapper.readTree(output);
    JsonNode fingerprint = json.path("fingerprint");

    if (!fingerprint.isArray() || fingerprint.isEmpty()) {
      throw new UnreadableAudioException("Audio file contains no analysable sound");
    }

    int[] points = new int[fingerprint.size()];
    for (int i = 0; i < points.length; i++) {
      points[i] = (int) fingerprint.get(i).asLong();
    }

    return new AudioFingerprint((int) Math.round(json.path("duration").asDouble()), points);
  }
}
