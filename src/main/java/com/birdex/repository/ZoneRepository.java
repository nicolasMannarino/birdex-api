package com.birdex.repository;

import com.birdex.entity.ZoneEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID> {

    Optional<ZoneEntity> findByName(String name);
    boolean existsByName(String name);
    List<ZoneEntity> findByNameContainingIgnoreCase(String q);
    List<ZoneEntity> findAllByNameIn(Collection<String> names);

    Page<ZoneEntity> findAllByNameContainingIgnoreCase(String q, Pageable pageable);

    @Query("""
           SELECT z
           FROM ZoneEntity z
           WHERE z.latitude BETWEEN :minLat AND :maxLat
             AND z.longitude BETWEEN :minLon AND :maxLon
           """)
    List<ZoneEntity> findInBBox(@Param("minLat") BigDecimal minLat,
                                @Param("maxLat") BigDecimal maxLat,
                                @Param("minLon") BigDecimal minLon,
                                @Param("maxLon") BigDecimal maxLon);


    @Query(value = """
            SELECT z.*
            FROM zones z
            JOIN bird_zone bz ON z.zone_id = bz.zone_id
            WHERE bz.bird_id = :birdId
            """,
            nativeQuery = true)
    List<ZoneEntity> findAllByBirdId(@Param("birdId") UUID birdId);
}
