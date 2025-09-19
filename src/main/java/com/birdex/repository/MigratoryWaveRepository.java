package com.birdex.repository;

import com.birdex.entity.MigratoryWaveEntity;
import com.birdex.entity.MigratoryWaveId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigratoryWaveRepository extends JpaRepository<MigratoryWaveEntity, MigratoryWaveId> {
    List<MigratoryWaveEntity> findByBird_BirdIdOrderByMonthAsc(UUID birdId);
}
