package com.birdex.repository;

import com.birdex.entity.RarityPointsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.UUID;

public interface RarityPointsRepository extends JpaRepository<RarityPointsEntity, UUID> {

    /**
     * Devuelve los puntos asociados a una rareza (case-insensitive).
     * Retorna Optional.empty() si no hay entrada.
     */
    @Query(value = """
        SELECT rp.points
        FROM rarity_points rp
        JOIN rarities r ON rp.rarity_id = r.rarity_id
        WHERE lower(r.name) = lower(:rarity)
        """, nativeQuery = true)
    Optional<Integer> findPointsByRarity(@Param("rarity") String rarity);
}
