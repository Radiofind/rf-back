package com.radiofind.track.library;

import org.springframework.data.domain.Sort;

public enum LibrarySort {
  TITLE("track.title"),
  DURATION("track.durationSeconds"),
  PLAYS("track.playsCount"),
  LIKES("track.likesCount"),
  ADDED("createdAt");

  private final String property;

  LibrarySort(String property) {
    this.property = property;
  }

  public Sort toSort(Sort.Direction direction) {
    Sort.Order order = new Sort.Order(direction, property).nullsLast();

    return Sort.by(this == TITLE ? order.ignoreCase() : order, Sort.Order.desc("id"));
  }
}
