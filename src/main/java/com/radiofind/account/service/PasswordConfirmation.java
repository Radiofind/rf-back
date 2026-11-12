package com.radiofind.account.service;

import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PasswordConfirmation {

  private final PasswordEncoder passwordEncoder;

  public void require(User user, String password) {
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
    }
  }
}
