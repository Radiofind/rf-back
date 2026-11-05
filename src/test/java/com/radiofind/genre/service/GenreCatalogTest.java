package com.radiofind.genre.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenreCatalogTest {

  private static final String CATALOG = """
      [
        {"code": "ROCK", "children": [{"code": "GRUNGE"}, {"code": "BLUES_ROCK"}]},
        {"code": "BLUES", "children": [{"code": "BLUES_ROCK"}]},
        {"code": "ELECTRONIC", "children": [
          {"code": "HOUSE", "children": [{"code": "HOUSE"}, {"code": "DEEP_HOUSE"}]},
          {"code": "SYNTHWAVE"}
        ]}
      ]
      """;

  @ParameterizedTest
  @CsvSource({
      "GRUNGE, true",
      "BLUES_ROCK, true",
      "DEEP_HOUSE, true",
      "SYNTHWAVE, true",
      "HOUSE, true",
      "ROCK, false",
      "ELECTRONIC, false",
      "deep_house, false",
      "POLKA_STEP, false"
  })
  void onlyGenresAreValidNeverTheirGroups(String code, boolean expected) {
    GenreCatalog catalog = new GenreCatalog(
        new ByteArrayResource(CATALOG.getBytes(StandardCharsets.UTF_8)), JsonMapper.builder().build());

    assertEquals(expected, catalog.isGenre(code));
  }

  @Test
  void bundledCatalogKnowsNestedGenres() {
    GenreCatalog catalog = new GenreCatalog(
        new ClassPathResource("genres/genre-catalog.json"), JsonMapper.builder().build());

    assertTrue(catalog.isGenre("DEEP_HOUSE"));
  }
}
