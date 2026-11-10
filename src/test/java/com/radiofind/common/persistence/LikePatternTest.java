package com.radiofind.common.persistence;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LikePatternTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "' Neon Lights '|%neon lights%",
      "100%|%100\\%%",
      "lost_in_space|%lost\\_in\\_space%",
      "AC\\DC|%ac\\\\dc%"
  })
  void searchTextMatchesLiterallyAnywhereIgnoringCase(String text, String expected) {
    String pattern = LikePattern.containingIgnoreCase(text);

    assertEquals(expected, pattern);
  }
}
