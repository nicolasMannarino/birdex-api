package com.birdex.repository;

import com.birdex.entity.BirdColor;
import com.birdex.entity.BirdColorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BirdColorRepository extends JpaRepository<BirdColor, BirdColorId> {

    @Query("""
        select c.name
        from BirdColor bc
        join bc.color c
        join bc.bird  b
        where lower(b.name) = lower(:name)
        order by c.name
    """)
    List<String> findColorNamesByBirdName(@Param("name") String name);
}
