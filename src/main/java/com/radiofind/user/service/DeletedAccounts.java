package com.radiofind.user.service;

import com.radiofind.user.entity.DeletedAccount;
import com.radiofind.user.repository.DeletedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletedAccounts {

  private static final Duration RETENTION = Duration.ofDays(30);

  private static final String DAILY_AT_THREE = "0 0 3 * * *";

  private final DeletedAccountRepository deletedAccountRepository;

  private final Clock clock;

  public void remember(UUID publicId) {
    deletedAccountRepository.save(new DeletedAccount(publicId, clock.instant()));
  }

  public boolean isRemembered(UUID publicId) {
    return deletedAccountRepository.existsByPublicIdAndDeletedAtAfter(publicId, cutoff());
  }

  @Scheduled(cron = DAILY_AT_THREE, zone = "UTC")
  @Transactional
  public void forgetExpired() {
    long forgotten = deletedAccountRepository.deleteByDeletedAtLessThanEqual(cutoff());

    if (forgotten > 0) {
      log.atInfo()
          .setMessage("deleted accounts forgotten")
          .addKeyValue("count", forgotten)
          .log();
    }
  }

  private Instant cutoff() {
    return clock.instant().minus(RETENTION);
  }
}
