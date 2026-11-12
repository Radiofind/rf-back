package com.radiofind.track.event;

import com.radiofind.track.entity.Track;
import com.radiofind.user.entity.User;

public record TrackDeletedEvent(long trackId, User owner, String title, String artistName, boolean accountClosed) {

  public static TrackDeletedEvent of(Track track) {
    return new TrackDeletedEvent(track.getId(), track.getOwner(), track.getTitle(), track.getArtistName(), false);
  }

  public static TrackDeletedEvent ofClosedAccount(Track track) {
    return new TrackDeletedEvent(track.getId(), track.getOwner(), track.getTitle(), track.getArtistName(), true);
  }
}
