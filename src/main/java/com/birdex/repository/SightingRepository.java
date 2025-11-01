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

    List<SightingEntity> findByUserEmailAndStateAndDeletedFalse(String email, String state);

    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailAndStateAndDeletedFalseOrderByDateTimeDesc(
            String birdName, String email, String state
    );

    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailNotAndStateAndDeletedFalseOrderByDateTimeDesc(
            String birdName, String email, String state
    );

    List<SightingEntity> findByUserEmailAndDeletedFalse(String email);
    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailAndDeletedFalseOrderByDateTimeDesc(
            String birdName, String email
    );
    List<SightingEntity> findByBird_NameIgnoreCaseAndUser_EmailNotAndDeletedFalseOrderByDateTimeDesc(String birdName, String email);
    @Query("""
        select s
        from SightingEntity s
        join s.bird b
        where s.deleted = false 
          and (:sizeLower   is null or lower(b.size) = :sizeLower)
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

    @Query("""
        select s
        from SightingEntity s
        join fetch s.bird b
        where s.deleted = false
        order by b.name asc, s.dateTime desc
        """)
    List<SightingEntity> findAllOrderByBirdAndDateTimeDesc();
    void deleteAllByIdInBatch(Iterable<UUID> ids);

    // Variante por IDs (asumiendo birdId y userId son UUID)
    @Query("""
        select s.sightingId
        from SightingEntity s
        where s.bird.birdId = :birdId
          and s.user.userId = :userId
          and s.deleted = false
          and s.state = 'CONFIRMED'
        order by s.dateTime desc
    """)
    List<UUID> findIdsByBirdIdAndUserId(@Param("birdId") UUID birdId,
                                        @Param("userId") UUID userId);
}
