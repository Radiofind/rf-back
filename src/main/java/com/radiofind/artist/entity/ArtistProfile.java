package com.radiofind.artist.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private ArtistType type;

  private String artistName;

  @Column(name = "short_description", length = 500)
  private String shortDescription;

  @Column(length = 3000)
  private String description;

  @ElementCollection
  @CollectionTable(name = "artist_main_genres", joinColumns = @JoinColumn(name = "artist_profile_id"))
  @Column(name = "genre", nullable = false, length = 100)
  @Builder.Default
  private List<String> mainGenres = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "artist_additional_genres", joinColumns = @JoinColumn(name = "artist_profile_id"))
  @Column(name = "genre", nullable = false, length = 100)
  @Builder.Default
  private List<String> additionalGenres = new ArrayList<>();

  @Embedded
  private ArtistSocialLinks socialLinks;

  @OneToOne
  @JoinColumn(name = "user_id")
  private User user;
}
