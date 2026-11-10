package com.radiofind.track.repository;

import com.radiofind.common.persistence.LikePattern;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TrackSpecifications {

  private TrackSpecifications() {
  }

  public static Specification<Track> ownedBy(Long ownerId) {
    return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
  }

  public static Specification<Track> inStatus(TrackStatus status) {
    if (status == null) {
      return Specification.unrestricted();
    }

    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  public static Specification<Track> titleOrArtistContains(String search) {
    if (search == null || search.isBlank()) {
      return Specification.unrestricted();
    }

    String pattern = LikePattern.containingIgnoreCase(search);

    return (root, query, cb) -> cb.or(
        cb.like(cb.lower(root.get("title")), pattern, LikePattern.ESCAPE),
        cb.like(cb.lower(root.get("artistName")), pattern, LikePattern.ESCAPE));
  }
}
