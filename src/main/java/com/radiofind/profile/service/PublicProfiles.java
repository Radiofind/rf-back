package com.radiofind.profile.service;

import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicProfiles {

  private final UserRepository userRepository;

  public User require(UUID publicId) {
    return userRepository.findByPublicId(publicId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
  }
}
