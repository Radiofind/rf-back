package com.radiofind.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deleted_accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeletedAccount {

  @Id
  @Column(name = "public_id", nullable = false, updatable = false)
  private UUID publicId;

  @Column(name = "deleted_at", nullable = false, updatable = false)
  private Instant deletedAt;
}
