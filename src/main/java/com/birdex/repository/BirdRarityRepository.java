package com.birdex.repository;


import com.birdex.entity.BirdRarityEntity;
import com.birdex.entity.BirdRarityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BirdRarityRepository extends JpaRepository<BirdRarityEntity, BirdRarityId> {


    @Query(value = """
        select r.name
        from bird_rarity br
        join rarities r on r.rarity_id = br.rarity_id
        join birds b    on b.bird_id   = br.bird_id
        where lower(b.name) = lower(:name)
        limit 1
    """, nativeQuery = true)
    Optional<String> findRarityNameByBirdName(@Param("name") String name);

    @Query("""
        select r.name
        from BirdRarityEntity br
        join br.rarity r
        join br.bird b
        where lower(b.name) like lower(concat('%', :partial, '%'))
        order by r.name
    """)
    List<String> findRarityNamesByBirdNameContaining(@Param("partial") String partial);
}
