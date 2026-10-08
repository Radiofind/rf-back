package com.radiofind.user.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.*;
import com.radiofind.user.entity.User;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  public CurrentUserResponse getCurrentUser(CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    return CurrentUserResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .build();
  }

  public UserProfileResponse getUserProfile(CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    return UserProfileResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .dateOfBirth(user.getDateOfBirth())
        .artistType(user.getArtistProfile() != null
            ? user.getArtistProfile().getType()
            : null)
        .artistName(user.getArtistProfile() != null
            ? user.getArtistProfile().getArtistName()
            : null)
        .description(user.getArtistProfile() != null
            ? user.getArtistProfile().getDescription()
            : null)
        .registeredAt(user.getCreatedAt())
        .build();
  }
}
