package com.radiofind.profile.service;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileCounters {

  private final FollowRepository followRepository;

  private final TrackRepository trackRepository;

  public long followers(long userId) {
    return followRepository.countFollowers(userId);
  }

  public long publishedTracks(long userId) {
    return trackRepository.countByOwnerIdAndStatus(userId, TrackStatus.APPROVED);
  }

  public long likesOfPublishedTracks(long userId) {
    return trackRepository.sumLikesOfOwnerInStatus(userId, TrackStatus.APPROVED);
  }
}
