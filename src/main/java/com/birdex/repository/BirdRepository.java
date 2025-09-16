package com.birdex.repository;

import com.birdex.entity.BirdEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.birdex.entity.BirdNamesView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BirdRepository extends JpaRepository<BirdEntity, UUID> {
    Optional<BirdEntity> findFirstByCommonNameContainingIgnoreCase(String commonName);

    String findRarityByName(String name);
    Optional<BirdEntity> findFirstByNameContainingIgnoreCase(String name);

    List<BirdNamesView> findAllProjectedBy();

    List<BirdNamesView> findAllByOrderByNameAsc();

    List<BirdNamesView> findDistinctBy();

    Page<BirdNamesView> findAllProjectedBy(Pageable pageable);
}
