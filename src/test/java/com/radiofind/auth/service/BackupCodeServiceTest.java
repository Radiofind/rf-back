package com.radiofind.auth.service;

import com.radiofind.auth.entity.BackupCode;
import com.radiofind.auth.repository.BackupCodeRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupCodeServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

  @Mock
  private BackupCodeRepository backupCodeRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  void codeTypedInLowerCaseWithSpacesIsRedeemed() {
    BackupCodeService service = new BackupCodeService(
        backupCodeRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    BackupCode backupCode = BackupCode.builder().codeHash("hash").build();
    when(backupCodeRepository.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(backupCode));
    when(passwordEncoder.matches("ABCDEFGHJK", "hash")).thenReturn(true);

    boolean redeemed = service.redeem(User.builder().id(7L).build(), " abcde fghjk ");

    assertTrue(redeemed);
    assertEquals(NOW, backupCode.getUsedAt());
  }

  @Test
  void unknownCodeIsNotRedeemed() {
    BackupCodeService service = new BackupCodeService(
        backupCodeRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    BackupCode backupCode = BackupCode.builder().codeHash("hash").build();
    when(backupCodeRepository.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(backupCode));
    when(passwordEncoder.matches("ABCDEFGHJK", "hash")).thenReturn(false);

    boolean redeemed = service.redeem(User.builder().id(7L).build(), "ABCDE-FGHJK");

    assertFalse(redeemed);
    assertNull(backupCode.getUsedAt());
  }
}
