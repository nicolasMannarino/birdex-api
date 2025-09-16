package com.birdex.repository;

import com.birdex.entity.BirdEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.birdex.entity.BirdNamesView;
import com.birdex.entity.BirdSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface BirdRepository extends JpaRepository<BirdEntity, UUID>, JpaSpecificationExecutor<BirdEntity> {
    Optional<BirdEntity> findFirstByCommonNameContainingIgnoreCase(String commonName);

    String findRarityByName(String name);

    Optional<BirdEntity> findFirstByNameContainingIgnoreCase(String name);

    List<BirdNamesView> findAllProjectedBy();

    List<BirdNamesView> findAllByOrderByNameAsc();

    List<BirdNamesView> findDistinctBy();

    Page<BirdNamesView> findAllProjectedBy(Pageable pageable);

    @Query(
            value = """
                    select distinct
                        b.bird_id   as birdId,
                        b.name      as name,
                        b.common_name as commonName,
                        b.image     as image,
                        b.size      as size
                    from birds b
                    left join bird_rarity br on br.bird_id = b.bird_id
                    left join rarities r      on r.rarity_id = br.rarity_id
                    left join bird_color bc   on bc.bird_id = b.bird_id
                    left join colors c        on c.color_id = bc.color_id
                    where (:rarity is null or lower(r.name) = lower(:rarity))
                      and (:color  is null or lower(c.name) = lower(:color))
                      and (:size   is null or lower(b.size) = lower(:size))
                    order by b.name
                    """,
            countQuery = """
                    select count(distinct b.bird_id)
                    from birds b
                    left join bird_rarity br on br.bird_id = b.bird_id
                    left join rarities r      on r.rarity_id = br.rarity_id
                    left join bird_color bc   on bc.bird_id = b.bird_id
                    left join colors c        on c.color_id = bc.color_id
                    where (:rarity is null or lower(r.name) = lower(:rarity))
                      and (:color  is null or lower(c.name) = lower(:color))
                      and (:size   is null or lower(b.size) = lower(:size))
                    """,
            nativeQuery = true
    )
    Page<BirdSummary> searchBirds(
            @Param("rarity") String rarity,
            @Param("color") String color,
            @Param("size") String size,
            Pageable pageable
    );
}
