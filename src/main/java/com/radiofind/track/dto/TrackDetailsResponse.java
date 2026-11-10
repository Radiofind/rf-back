package com.radiofind.track.dto;

import com.radiofind.storage.service.AudioType;
import com.radiofind.track.entity.TrackStatus;

import java.time.Instant;
import java.util.List;

public record TrackDetailsResponse(
    Long id,
    String title,
    String artistName,
    String uploadedBy,
    List<String> genres,
    String description,
    TrackStatus status,
    Integer durationSeconds,
    AudioType audioFormat,
    long audioSizeBytes,
    String coverUrl,
    Instant createdAt,
    Instant updatedAt) {
}
