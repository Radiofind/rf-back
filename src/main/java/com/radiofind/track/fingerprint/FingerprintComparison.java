package com.radiofind.track.fingerprint;

final class FingerprintComparison {

  private static final int BITS_PER_POINT = 32;

  private FingerprintComparison() {
  }

  static double similarity(int[] first, int[] second, int maxOffset, int minOverlap) {
    double best = 0;

    for (int offset = -maxOffset; offset <= maxOffset; offset++) {
      best = Math.max(best, similarityAt(first, second, offset, minOverlap));
    }

    return best;
  }

  private static double similarityAt(int[] first, int[] second, int offset, int minOverlap) {
    int start = Math.max(0, offset);
    int end = Math.min(first.length, second.length + offset);
    int overlap = end - start;

    if (overlap < minOverlap) {
      return 0;
    }

    long differentBits = 0;
    for (int i = start; i < end; i++) {
      differentBits += Integer.bitCount(first[i] ^ second[i - offset]);
    }

    return 1 - (double) differentBits / ((long) overlap * BITS_PER_POINT);
  }
}
