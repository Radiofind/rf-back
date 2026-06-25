package com.radiofind.artist.entity;

import com.radiofind.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

  @Column(length = 3000)
  private String description;

  @OneToOne
  @JoinColumn(name = "user_id")
  private User user;
}
