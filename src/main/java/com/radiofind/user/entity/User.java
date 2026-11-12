package com.radiofind.user.entity;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.auth.entity.TwoFactorMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "two_factor_method", nullable = false, length = 30)
  @ColumnDefault("'EMAIL'")
  @Builder.Default
  private TwoFactorMethod twoFactorMethod = TwoFactorMethod.EMAIL;

  @Column(name = "totp_secret")
  private String totpSecret;

  @Column(name = "totp_last_used_step")
  private Long totpLastUsedStep;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private ArtistProfile artistProfile;
}
