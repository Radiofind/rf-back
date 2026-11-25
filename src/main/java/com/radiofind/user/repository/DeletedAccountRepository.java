package com.radiofind.user.repository;

import com.radiofind.user.entity.DeletedAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface DeletedAccountRepository extends JpaRepository<DeletedAccount, UUID> {

  boolean existsByPublicIdAndDeletedAtAfter(UUID publicId, Instant cutoff);

  long deleteByDeletedAtLessThanEqual(Instant cutoff);
}
