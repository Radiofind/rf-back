package com.radiofind.storage.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarImageTypeTest {

  private Optional<AvatarImageType> detect(byte[] content) {
    return AvatarImageType.detect(new MockMultipartFile("file", "avatar.png", "image/png", content));
  }

  @Test
  void detectsSupportedImagesByContent() {
    assertEquals(Optional.of(AvatarImageType.PNG), detect(TestImages.PNG));
    assertEquals(Optional.of(AvatarImageType.JPEG), detect(TestImages.JPEG));
    assertEquals(Optional.of(AvatarImageType.WEBP), detect(TestImages.WEBP));
  }

  @Test
  void rejectsContentThatIsNotASupportedImage() {
    assertEquals(Optional.empty(), detect(TestImages.HTML));
    assertEquals(Optional.empty(), detect(new byte[] {(byte) 0xFF, (byte) 0xD8}));
    assertEquals(Optional.empty(), detect("RIFF\0\0\0\0WAVEfmt ".getBytes()));
  }

  @Test
  void resolvesMediaTypeOnlyForImageExtensions() {
    assertEquals(MediaType.IMAGE_JPEG, AvatarImageType.fromFileName("legacy.JPEG").orElseThrow().getMediaType());
    assertEquals(MediaType.IMAGE_PNG, AvatarImageType.fromFileName("a.png").orElseThrow().getMediaType());
    assertEquals(Optional.empty(), AvatarImageType.fromFileName("evil.html"));
    assertEquals(Optional.empty(), AvatarImageType.fromFileName("evil.svg"));
  }
}
