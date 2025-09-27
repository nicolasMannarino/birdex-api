package com.birdex.jobs;

import com.birdex.entity.SightingEntity;
import com.birdex.repository.SightingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SightingCleanupJob {

    private final SightingRepository sightingRepository;

    private static final double GRID_METERS = 250.0;
    private static final int KEEP_PER_CELL = 5;

    /**
     * Corre cada 15 minutos, en el minuto 0, 15, 30, 45.
     * Cron Spring: s m h d M dow
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void run() {
        LocalDateTime start = LocalDateTime.now();
        log.info("[SightingCleanupJob] Inicio limpieza de avistajes duplicados cercanos…");

        List<SightingEntity> all = sightingRepository.findAllOrderByBirdAndDateTimeDesc();
        if (all.isEmpty()) {
            log.info("[SightingCleanupJob] No hay avistajes. Fin.");
            return;
        }

        Map<String, List<SightingEntity>> byBird = new LinkedHashMap<>();
        for (SightingEntity s : all) {
            String birdKey = s.getBird().getName();
            byBird.computeIfAbsent(birdKey, k -> new ArrayList<>()).add(s);
        }

        List<UUID> toDelete = new ArrayList<>();
        AtomicInteger cellsCount = new AtomicInteger(0);

        for (Map.Entry<String, List<SightingEntity>> e : byBird.entrySet()) {
            String birdName = e.getKey();
            List<SightingEntity> sightings = e.getValue();

            Map<String, List<SightingEntity>> byCell = new HashMap<>();

            for (SightingEntity s : sightings) {
                if (s.getLatitude() == null || s.getLongitude() == null) continue;

                String cellKey = cellKeyFor(s.getLatitude(), s.getLongitude(), GRID_METERS);
                byCell.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(s);
            }

            for (Map.Entry<String, List<SightingEntity>> cellEntry : byCell.entrySet()) {
                List<SightingEntity> list = cellEntry.getValue();

                if (list.size() > KEEP_PER_CELL) {
                    list.sort(Comparator.comparing(SightingEntity::getDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
                    List<SightingEntity> excess = list.subList(KEEP_PER_CELL, list.size());
                    for (SightingEntity old : excess) {
                        toDelete.add(old.getSightingId());
                    }
                }
            }

            cellsCount.addAndGet(byCell.size());
            log.debug("[SightingCleanupJob] Ave='{}': celdas={}, candidatosABorrar={}",
                    birdName, byCell.size(), toDelete.size());
        }

        if (!toDelete.isEmpty()) {
            sightingRepository.deleteAllByIdInBatch(toDelete);
        }

        log.info("[SightingCleanupJob] Fin. aves={}, celdas={}, borrados={}, duración={}ms",
                byBird.size(), cellsCount.get(), toDelete.size(),
                java.time.Duration.between(start, LocalDateTime.now()).toMillis());
    }


    private static String cellKeyFor(BigDecimal lat, BigDecimal lon, double gridMeters) {
        double dLat = lat.doubleValue();
        double dLon = lon.doubleValue();


        double degPerMeterLat = 1.0 / 111_320.0;
        double cellDegLat = gridMeters * degPerMeterLat;


        double degPerMeterLon = 1.0 / (111_320.0 * Math.cos(Math.toRadians(dLat)));

        if (!Double.isFinite(degPerMeterLon) || degPerMeterLon <= 0) {
            degPerMeterLon = 1.0 / 111_320.0;
        }
        double cellDegLon = gridMeters * degPerMeterLon;

        long gx = (long) Math.floor(dLat / cellDegLat);
        long gy = (long) Math.floor(dLon / cellDegLon);

        return gx + ":" + gy;
    }
}