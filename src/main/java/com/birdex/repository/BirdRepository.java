package com.birdex.repository;

import com.birdex.entity.BirdEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface BirdRepository extends JpaRepository<BirdEntity, UUID> {
    @Query(value = "SELECT * FROM birds b WHERE unaccent(lower(b.common_name)) LIKE unaccent(lower(CONCAT('%', :commonName, '%'))) LIMIT 1", nativeQuery = true)
    Optional<BirdEntity> findByCommonNameLike(String commonName);

    @Query(value = "SELECT * FROM birds b WHERE unaccent(lower(b.name)) LIKE unaccent(lower(CONCAT('%', :name, '%'))) LIMIT 1", nativeQuery = true)
    Optional<BirdEntity> findByName(String name);

}
