package com.radiofind.follow.service;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.user.event.AccountDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowCleanupTest {

  @Mock
  private FollowRepository followRepository;

  @InjectMocks
  private FollowCleanup followCleanup;

  @Test
  void deletedAccountLosesItsFollowsInBothDirections() {
    followCleanup.forgetDeletedAccount(new AccountDeletedEvent(7L));

    verify(followRepository).deleteByUserId(7L);
  }
}
