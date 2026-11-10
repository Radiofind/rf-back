package com.radiofind.track.dto;

import org.springframework.data.domain.Sort;

public enum OwnTracksSort {
  TITLE("title"),
  DURATION("durationSeconds"),
  PLAYS("playsCount"),
  LIKES("likesCount"),
  UPLOADED("createdAt");

  private final String property;

  OwnTracksSort(String property) {
    this.property = property;
  }

  public Sort toSort(Sort.Direction direction) {
    Sort.Order order = new Sort.Order(direction, property).nullsLast();

    return Sort.by(this == TITLE ? order.ignoreCase() : order, Sort.Order.desc("id"));
  }
}
