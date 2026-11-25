package com.radiofind.follow.service;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.user.event.AccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowCleanup {

  private final FollowRepository followRepository;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    followRepository.deleteByUserId(event.userId());
  }
}
