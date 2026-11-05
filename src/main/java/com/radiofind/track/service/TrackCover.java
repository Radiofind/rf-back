package com.radiofind.track.service;

import org.springframework.http.MediaType;

import java.nio.file.Path;

public record TrackCover(Path path, MediaType mediaType) {
}
