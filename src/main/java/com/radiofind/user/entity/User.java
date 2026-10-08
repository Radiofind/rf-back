package com.radiofind.user.entity;

import com.radiofind.artist.entity.ArtistProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String surname;

  @Column(unique = true, nullable = false)
  private String email;

  private String recoveryEmail;

  @Column(nullable = false)
  private LocalDate dateOfBirth;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private String avatarFileName;

  @Enumerated(EnumType.STRING)
  private SubscriptionType subscriptionType;

  private int uploadsThisMonth;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private ArtistProfile artistProfile;
}
