package com.radiofind.track.fingerprint;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FingerprintComparisonTest {

  @Test
  void sameRecordingIsFullySimilar() {
    int[] points = new Random(1).ints(200).toArray();

    assertEquals(1.0, FingerprintComparison.similarity(points, points.clone(), 10, 40));
  }

  @Test
  void recordingWithAnExtraIntroIsFoundAtItsOffset() {
    int[] points = new Random(1).ints(200).toArray();
    int[] withIntro = new int[205];
    System.arraycopy(points, 0, withIntro, 5, points.length);

    assertEquals(1.0, FingerprintComparison.similarity(withIntro, points, 10, 40));
  }

  @Test
  void unrelatedRecordingsShareAboutHalfOfTheirBits() {
    int[] first = new Random(1).ints(200).toArray();
    int[] second = new Random(2).ints(200).toArray();

    double similarity = FingerprintComparison.similarity(first, second, 10, 40);

    assertTrue(similarity > 0.45 && similarity < 0.6, "similarity was " + similarity);
  }

  @Test
  void recordingsTooShortToOverlapAreNotSimilar() {
    int[] points = new Random(1).ints(30).toArray();

    assertEquals(0.0, FingerprintComparison.similarity(points, Arrays.copyOf(points, 30), 10, 40));
  }
}
