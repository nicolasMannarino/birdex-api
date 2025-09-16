package com.birdex.repository;

import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.BirdEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SightingRepository extends JpaRepository<SightingEntity, UUID> {
    List<SightingEntity> findByUserEmail(String email);
    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailOrderByDateTimeDesc(String birdName, String email);
    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailNotOrderByDateTimeDesc(String birdName, String email);
    @Query("""
        select s
        from SightingEntity s
        join s.bird b
        where (:sizeLower   is null or lower(b.size) = :sizeLower)
          and (:rarityLower is null or exists (
                 select 1
                 from BirdRarityEntity br
                 join br.rarity r
                 where br.bird = b
                   and lower(r.name) = :rarityLower
          ))
          and (:colorLower  is null or exists (
                 select 1
                 from BirdColor bc
                 join bc.color c
                 where bc.bird = b
                   and lower(c.name) = :colorLower
          ))
        order by s.dateTime desc
        """)
    Page<SightingEntity> searchSightings(
            @Param("rarityLower") String rarityLower,
            @Param("colorLower")  String colorLower,
            @Param("sizeLower")   String sizeLower,
            Pageable pageable
    );
}
