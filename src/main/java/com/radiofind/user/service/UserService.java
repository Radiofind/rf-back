package com.radiofind.user.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.CurrentUserResponse;
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
}
