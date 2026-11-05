package com.radiofind.storage.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageTypeTest {

  private Optional<ImageType> detect(byte[] content) {
    return ImageType.detect(new MockMultipartFile("file", "avatar.png", "image/png", content));
  }

  @Test
  void detectsSupportedImagesByContent() {
    assertEquals(Optional.of(ImageType.PNG), detect(TestImages.PNG));
    assertEquals(Optional.of(ImageType.JPEG), detect(TestImages.JPEG));
    assertEquals(Optional.of(ImageType.WEBP), detect(TestImages.WEBP));
  }

  @Test
  void rejectsContentThatIsNotASupportedImage() {
    assertEquals(Optional.empty(), detect(TestImages.HTML));
    assertEquals(Optional.empty(), detect(new byte[] {(byte) 0xFF, (byte) 0xD8}));
    assertEquals(Optional.empty(), detect("RIFF\0\0\0\0WAVEfmt ".getBytes()));
  }

  @Test
  void resolvesMediaTypeOnlyForImageExtensions() {
    assertEquals(MediaType.IMAGE_JPEG, ImageType.fromFileName("legacy.JPEG").orElseThrow().getMediaType());
    assertEquals(MediaType.IMAGE_PNG, ImageType.fromFileName("a.png").orElseThrow().getMediaType());
    assertEquals(Optional.empty(), ImageType.fromFileName("evil.html"));
    assertEquals(Optional.empty(), ImageType.fromFileName("evil.svg"));
  }
}
