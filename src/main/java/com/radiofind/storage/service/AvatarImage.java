package com.radiofind.storage.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record AvatarImage(Resource resource, MediaType mediaType) {
}
