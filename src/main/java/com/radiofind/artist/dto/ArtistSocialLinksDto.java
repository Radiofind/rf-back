package com.radiofind.artist.dto;

import com.radiofind.artist.entity.ArtistSocialLinks;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSocialLinksDto {

  @Size(max = 2048)
  private String telegram;

  @Size(max = 2048)
  private String instagram;

  @Size(max = 2048)
  private String youtube;

  @Size(max = 2048)
  private String facebook;

  @Size(max = 2048)
  private String spotify;

  @Size(max = 2048)
  private String appleMusic;

  @Size(max = 2048)
  private String soundcloud;

  @Size(max = 2048)
  private String bandcamp;

  @Size(max = 2048)
  private String icloud;

  @Size(max = 2048)
  private String tiktok;

  public static ArtistSocialLinksDto from(ArtistSocialLinks links) {

    if (links == null) {
      return null;
    }

    return ArtistSocialLinksDto.builder()
        .telegram(links.getTelegram())
        .instagram(links.getInstagram())
        .youtube(links.getYoutube())
        .facebook(links.getFacebook())
        .spotify(links.getSpotify())
        .appleMusic(links.getAppleMusic())
        .soundcloud(links.getSoundcloud())
        .bandcamp(links.getBandcamp())
        .icloud(links.getIcloud())
        .tiktok(links.getTiktok())
        .build();
  }
}
