package com.radiofind.artist.entity;

public enum ArtistType {
  ARTIST,
  BAND;

  // Every user has an artist profile; the type falls back to ARTIST when none is chosen
  public static final ArtistType DEFAULT = ARTIST;
}
