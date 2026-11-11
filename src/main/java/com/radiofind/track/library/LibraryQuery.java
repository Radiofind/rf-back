package com.radiofind.track.library;

import org.springframework.data.domain.Sort;

public record LibraryQuery(
    String search,
    LibrarySort sort,
    Sort.Direction direction,
    int page,
    int size) {
}
