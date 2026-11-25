package com.radiofind.profile.service;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileFollowService {

  private final PublicProfiles publicProfiles;

  private final ProfileAccessResolver profileAccessResolver;

  private final FollowRepository followRepository;

  private final Clock clock;

  @Transactional
  public void follow(CustomUserDetails follower, UUID publicId) {
    User followee = publicProfiles.require(publicId);
    long followerId = follower.getUser().getId();

    if (followee.getId() == followerId) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself");
    }

    if (!profileAccessResolver.resolve(followerId, followee).offersFollowing()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This profile cannot be followed");
    }

    followRepository.insertIfAbsent(followerId, followee.getId(), clock.instant());
  }

  @Transactional
  public void unfollow(CustomUserDetails follower, UUID publicId) {
    User followee = publicProfiles.require(publicId);

    followRepository.deleteByPair(follower.getUser().getId(), followee.getId());
  }
}
