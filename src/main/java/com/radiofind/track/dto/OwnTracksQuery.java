package com.radiofind.track.dto;

import com.radiofind.track.entity.TrackStatus;
import org.springframework.data.domain.Sort;

public record OwnTracksQuery(
    TrackStatus status,
    String search,
    OwnTracksSort sort,
    Sort.Direction direction,
    int page,
    int size) {
}
