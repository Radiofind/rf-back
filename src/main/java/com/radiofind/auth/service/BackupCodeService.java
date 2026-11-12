package com.radiofind.auth.service;

import com.radiofind.auth.entity.BackupCode;
import com.radiofind.auth.repository.BackupCodeRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.event.AccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class BackupCodeService {

  private static final int CODE_COUNT = 10;
  private static final int GROUP_LENGTH = 5;
  private static final String ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
  private static final String GROUP_SEPARATOR = "-";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final BackupCodeRepository backupCodeRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Transactional
  public List<String> regenerate(User user) {
    backupCodeRepository.deleteByUserId(user.getId());

    List<String> codes = IntStream.range(0, CODE_COUNT).mapToObj(index -> generateCode()).toList();

    backupCodeRepository.saveAll(codes.stream()
        .map(code -> BackupCode.builder()
            .user(user)
            .codeHash(passwordEncoder.encode(normalize(code)))
            .build())
        .toList());

    return codes;
  }

  @Transactional
  public boolean redeem(User user, String code) {
    String normalized = normalize(code);

    return backupCodeRepository.findByUserIdAndUsedAtIsNull(user.getId()).stream()
        .filter(backupCode -> passwordEncoder.matches(normalized, backupCode.getCodeHash()))
        .findFirst()
        .map(backupCode -> {
          backupCode.setUsedAt(Instant.now(clock));
          return true;
        })
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public long countRemaining(long userId) {
    return backupCodeRepository.countByUserIdAndUsedAtIsNull(userId);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    backupCodeRepository.deleteByUserId(event.userId());
  }

  private String generateCode() {
    return randomGroup() + GROUP_SEPARATOR + randomGroup();
  }

  private String randomGroup() {
    StringBuilder group = new StringBuilder(GROUP_LENGTH);

    for (int index = 0; index < GROUP_LENGTH; index++) {
      group.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
    }

    return group.toString();
  }

  private String normalize(String code) {
    return code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }
}
