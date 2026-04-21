package com.radiofind.track.service;

import com.radiofind.track.entity.*;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackService {

  private final TrackRepository trackRepository;
  private final UserRepository userRepository;

  private final String UPLOAD_DIR = "/uploads";

  public Track upload(MultipartFile file, String title) {

    String email = SecurityContextHolder.getContext().getAuthentication().getName();

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

    try {
      File dir = new File(UPLOAD_DIR);
      if (!dir.exists())
        dir.mkdirs();

      file.transferTo(new File(UPLOAD_DIR + filename));
    } catch (Exception e) {
      throw new RuntimeException("File upload failed");
    }

    Track track = Track.builder()
        .title(title)
        .fileUrl(UPLOAD_DIR + filename)
        .status(TrackStatus.PENDING)
        .uploadedAt(LocalDateTime.now())
        .user(user)
        .build();

    return trackRepository.save(track);
  }
}
