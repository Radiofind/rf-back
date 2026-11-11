package com.radiofind.track.library;

import com.radiofind.common.persistence.LikePattern;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackLike;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

final class LibrarySpecifications {

  private LibrarySpecifications() {
  }

  static Specification<TrackLike> likedBy(Long userId) {
    return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
  }

  static Specification<TrackLike> titleOrArtistContains(String search) {
    if (search == null || search.isBlank()) {
      return Specification.unrestricted();
    }

    String pattern = LikePattern.containingIgnoreCase(search);

    return (root, query, cb) -> {
      Path<Track> track = root.get("track");

      return cb.or(
          cb.like(cb.lower(track.get("title")), pattern, LikePattern.ESCAPE),
          cb.like(cb.lower(track.get("artistName")), pattern, LikePattern.ESCAPE));
    };
  }
}
