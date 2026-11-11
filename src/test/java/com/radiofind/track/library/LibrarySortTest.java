package com.radiofind.track.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarySortTest {

  @ParameterizedTest
  @CsvSource({
      "DURATION, track.durationSeconds",
      "PLAYS, track.playsCount",
      "LIKES, track.likesCount",
      "ADDED, createdAt"
  })
  void numbersAndDatesAreSortedAsTheyAreWithUnknownValuesLast(LibrarySort sort, String property) {
    Sort.Order order = sort.toSort(Sort.Direction.DESC).getOrderFor(property);

    assertEquals(Sort.Direction.DESC, order.getDirection());
    assertEquals(Sort.NullHandling.NULLS_LAST, order.getNullHandling());
    assertFalse(order.isIgnoreCase());
  }

  @Test
  void titlesAreSortedIgnoringCase() {
    Sort.Order order = LibrarySort.TITLE.toSort(Sort.Direction.ASC).getOrderFor("track.title");

    assertEquals(Sort.Direction.ASC, order.getDirection());
    assertTrue(order.isIgnoreCase());
  }

  @Test
  void equalValuesKeepTheLatestAdditionFirst() {
    Sort sort = LibrarySort.LIKES.toSort(Sort.Direction.ASC);

    assertEquals(Sort.Order.desc("id"), sort.getOrderFor("id"));
  }
}
