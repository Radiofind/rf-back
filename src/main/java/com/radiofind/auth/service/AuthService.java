package com.radiofind.auth.service;

import com.radiofind.auth.dto.*;
import com.radiofind.security.JwtService;
import com.radiofind.user.entity.*;
import com.radiofind.user.repository.UserRepository;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final ArtistProfileRepository artistProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthResponse register(RegisterRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    User user = User.builder()
        .name(request.getName())
        .surname(request.getSurname())
        .email(request.getEmail())
        .recoveryEmail(request.getRecoveryEmail())
        .dateOfBirth(request.getDateOfBirth())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .subscriptionType(SubscriptionType.FREE)
        .uploadsThisMonth(0)
        .build();

    userRepository.save(user);

    if (request.getArtistInformation() != null) {

      ArtistProfile artistProfile = ArtistProfile.builder()
          .type(request.getArtistInformation().getTypeOfArtist())
          .artistName(request.getArtistInformation().getArtistName())
          .description(request.getArtistInformation().getDescription())
          .user(user)
          .build();

      artistProfileRepository.save(artistProfile);

      user.setArtistProfile(artistProfile);

      userRepository.save(user);
    }

    String token = jwtService.generateToken(user);

    return AuthResponse.builder()
        .token(token)
        .build();
  }

  public AuthResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid password");
    }

    String token = jwtService.generateToken(user);

    return new AuthResponse(token);
  }
}
