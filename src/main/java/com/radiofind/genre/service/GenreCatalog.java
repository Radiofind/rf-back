package com.radiofind.genre.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GenreCatalog {

  private final Set<String> genres;

  public GenreCatalog(
      @Value("classpath:genres/genre-catalog.json") Resource catalog,
      ObjectMapper objectMapper) {
    Set<String> leaves = new HashSet<>();
    collectLeaves(read(catalog, objectMapper), leaves);
    this.genres = Set.copyOf(leaves);
  }

  // Group nodes only structure the catalog and are never a valid value
  public boolean isGenre(String code) {
    return genres.contains(code);
  }

  private static List<GenreNode> read(Resource catalog, ObjectMapper objectMapper) {
    try (InputStream inputStream = catalog.getInputStream()) {
      return objectMapper.readValue(inputStream, new TypeReference<List<GenreNode>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read genre catalog", e);
    }
  }

  private static void collectLeaves(List<GenreNode> nodes, Set<String> leaves) {
    for (GenreNode node : nodes) {
      if (node.children() == null || node.children().isEmpty()) {
        leaves.add(node.code());
      } else {
        collectLeaves(node.children(), leaves);
      }
    }
  }

  private record GenreNode(String code, List<GenreNode> children) {
  }
}
