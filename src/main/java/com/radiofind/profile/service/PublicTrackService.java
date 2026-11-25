package com.radiofind.profile.service;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.repository.TrackSpecifications;
import com.radiofind.track.service.TrackListing;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicTrackService {

  private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final PublicProfiles publicProfiles;

  private final ProfileAccessResolver profileAccessResolver;

  private final TrackRepository trackRepository;

  private final TrackListing trackListing;

  @Transactional(readOnly = true)
  public PageResponse<TrackResponse> getTracks(CustomUserDetails viewer, UUID publicId, int page, int size) {
    User owner = publicProfiles.require(publicId);
    long viewerId = viewer.getUser().getId();

    if (!profileAccessResolver.resolve(viewerId, owner).showsDetails()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profile is private");
    }

    Specification<Track> published = Specification.allOf(
        TrackSpecifications.ownedBy(owner.getId()),
        TrackSpecifications.inStatus(TrackStatus.APPROVED));

    return trackListing.forListener(
        trackRepository.findAll(published, PageRequest.of(page, size, NEWEST_FIRST)),
        viewerId);
  }
}
