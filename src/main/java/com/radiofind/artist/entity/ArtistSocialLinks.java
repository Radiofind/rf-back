package com.radiofind.artist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistSocialLinks {

  @Column(name = "telegram_url", length = 2048)
  private String telegram;

  @Column(name = "instagram_url", length = 2048)
  private String instagram;

  @Column(name = "youtube_url", length = 2048)
  private String youtube;

  @Column(name = "facebook_url", length = 2048)
  private String facebook;

  @Column(name = "spotify_url", length = 2048)
  private String spotify;

  @Column(name = "apple_music_url", length = 2048)
  private String appleMusic;

  @Column(name = "soundcloud_url", length = 2048)
  private String soundcloud;

  @Column(name = "bandcamp_url", length = 2048)
  private String bandcamp;

  @Column(name = "icloud_url", length = 2048)
  private String icloud;

  @Column(name = "tiktok_url", length = 2048)
  private String tiktok;
}
