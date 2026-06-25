package com.radiofind.artist.repository;

import com.radiofind.artist.entity.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {

}
