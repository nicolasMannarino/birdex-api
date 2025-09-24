package com.birdex.repository;

import com.birdex.entity.BirdEntity;
import com.birdex.entity.BirdNamesView;
import com.birdex.entity.BirdSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BirdRepository extends JpaRepository<BirdEntity, UUID>, JpaSpecificationExecutor<BirdEntity> {

    Optional<BirdEntity> findFirstByCommonNameContainingIgnoreCase(String commonName);

    @EntityGraph(attributePaths = {"migratoryWaves", "migratoryWaves.province"})
    Optional<BirdEntity> findFirstByNameContainingIgnoreCase(String name);

    List<BirdNamesView> findAllProjectedBy();

    List<BirdNamesView> findAllByOrderByNameAsc();

    List<BirdNamesView> findDistinctBy();

    Page<BirdNamesView> findAllProjectedBy(Pageable pageable);

    @Query(
            value = """
        select
            b.bird_id     as birdId,
            b.name        as name,
            b.common_name as commonName,
            b.image       as image,
            b.size        as size,
            b.length_min_mm as lengthMinMm,
            b.length_max_mm as lengthMaxMm,
            b.weight_min_g  as weightMinG,
            b.weight_max_g  as weightMaxG,
            r.name          as rarity,
            array_remove(array_agg(distinct c.name), null) as colors
        from birds b
        left join bird_rarity br on br.bird_id = b.bird_id
        left join rarities r      on r.rarity_id = br.rarity_id
        left join bird_color bc   on bc.bird_id = b.bird_id
        left join colors c        on c.color_id = bc.color_id
        where
          (:rarity is null or unaccent(btrim(r.name)) ILIKE unaccent(btrim(:rarity)))
          and (:color  is null or unaccent(btrim(c.name)) ILIKE unaccent(btrim(:color)))
          and (:size   is null or unaccent(btrim(b.size)) ILIKE unaccent(btrim(:size)))
          and ( :lengthMinMm is null or (b.length_max_mm is not null and b.length_max_mm >= :lengthMinMm) )
          and ( :lengthMaxMm is null or (b.length_min_mm is not null and b.length_min_mm <= :lengthMaxMm) )
          and ( :weightMinG is null  or (b.weight_max_g  is not null and b.weight_max_g  >= :weightMinG) )
          and ( :weightMaxG is null  or (b.weight_min_g  is not null and b.weight_min_g  <= :weightMaxG) )
        group by
          b.bird_id, b.name, b.common_name, b.image, b.size,
          b.length_min_mm, b.length_max_mm, b.weight_min_g, b.weight_max_g,
          r.name
        order by b.name
        """,
            countQuery = """
        select count(distinct b.bird_id)
        from birds b
        left join bird_rarity br on br.bird_id = b.bird_id
        left join rarities r      on r.rarity_id = br.rarity_id
        left join bird_color bc   on bc.bird_id = b.bird_id
        left join colors c        on c.color_id = bc.color_id
        where
          ( :rarity is null or unaccent(lower(btrim(r.name))) = unaccent(lower(btrim(:rarity))) )
          and ( :color  is null or unaccent(lower(btrim(c.name))) = unaccent(lower(btrim(:color))) )
          and ( :size   is null or unaccent(lower(btrim(b.size))) = unaccent(lower(btrim(:size))) )
          and ( :lengthMinMm is null or (b.length_max_mm is not null and b.length_max_mm >= :lengthMinMm) )
          and ( :lengthMaxMm is null or (b.length_min_mm is not null and b.length_min_mm <= :lengthMaxMm) )
          and ( :weightMinG is null  or (b.weight_max_g  is not null and b.weight_max_g  >= :weightMinG) )
          and ( :weightMaxG is null  or (b.weight_min_g  is not null and b.weight_min_g  <= :weightMaxG) )
        """,
            nativeQuery = true
    )
    Page<BirdSummary> searchBirds(
            @Param("rarity") String rarity,
            @Param("color")  String color,
            @Param("size")   String size,
            @Param("lengthMinMm") Integer lengthMinMm,
            @Param("lengthMaxMm") Integer lengthMaxMm,
            @Param("weightMinG")  Integer weightMinG,
            @Param("weightMaxG")  Integer weightMaxG,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"migratoryWaves", "migratoryWaves.province"})
    Optional<BirdEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"migratoryWaves", "migratoryWaves.province"})
    List<BirdEntity> findAll();

    @EntityGraph(attributePaths = {"migratoryWaves", "migratoryWaves.province"})
    Optional<BirdEntity> findByName(String name);
}