package com.radiofind.storage.service;

import java.nio.charset.StandardCharsets;

public final class TestImages {

  public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

  public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0};

  public static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

  public static final byte[] HTML = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.US_ASCII);

  private TestImages() {
  }
}
