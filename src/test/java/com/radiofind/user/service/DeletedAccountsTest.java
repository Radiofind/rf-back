package com.radiofind.user.service;

import com.radiofind.user.entity.DeletedAccount;
import com.radiofind.user.repository.DeletedAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletedAccountsTest {

  @Mock
  private DeletedAccountRepository deletedAccountRepository;

  @Test
  void deletedAccountIsRememberedWithTheMomentOfDeletion() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
    DeletedAccounts deletedAccounts = new DeletedAccounts(deletedAccountRepository, clock);
    UUID publicId = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");

    deletedAccounts.remember(publicId);

    ArgumentCaptor<DeletedAccount> saved = ArgumentCaptor.forClass(DeletedAccount.class);
    verify(deletedAccountRepository).save(saved.capture());
    assertEquals(publicId, saved.getValue().getPublicId());
    assertEquals(Instant.parse("2026-11-25T10:00:00Z"), saved.getValue().getDeletedAt());
  }

  @Test
  void deletedAccountIsRecognisedOnlyWithinThirtyDays() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
    DeletedAccounts deletedAccounts = new DeletedAccounts(deletedAccountRepository, clock);
    UUID publicId = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");
    when(deletedAccountRepository.existsByPublicIdAndDeletedAtAfter(publicId,
        Instant.parse("2026-10-26T10:00:00Z"))).thenReturn(true);

    boolean remembered = deletedAccounts.isRemembered(publicId);

    assertTrue(remembered);
  }

  @Test
  void deletedAccountsOlderThanThirtyDaysAreForgotten() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
    DeletedAccounts deletedAccounts = new DeletedAccounts(deletedAccountRepository, clock);

    deletedAccounts.forgetExpired();

    verify(deletedAccountRepository).deleteByDeletedAtLessThanEqual(Instant.parse("2026-10-26T10:00:00Z"));
  }
}
