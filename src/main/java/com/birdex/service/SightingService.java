package com.birdex.service;

import com.birdex.domain.*;
import com.birdex.dto.SightingDto;
import com.birdex.dto.SightingFullResponse;
import com.birdex.dto.SightingsForBirdResponse;
import com.birdex.dto.enums.SightingStatus;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.BirdNotFoundException;
import com.birdex.exception.UserNotFoundException;
import com.birdex.mapper.SightingMapper;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private final BucketService bucketService;
    private final BirdRarityRepository birdRarityRepository;
    private final PointsService pointsService;
    private final MissionService missionService;
    private final AchievementService achievementService;

    private static final String CACHE = "public, max-age=31536000, immutable";

    public void registerSighting(SightingRequest request) {
        log.info("📸 Registrando avistamiento confirmado para usuario: {}", request.getEmail());

        // Buscar usuario
        UserEntity userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("No user found for email: {}", request.getEmail());
                    return new UserNotFoundException(request.getEmail());
                });

        // Buscar ave
        BirdEntity birdEntity = birdRepository.findFirstByNameContainingIgnoreCase(request.getBirdName())
                .orElseThrow(() -> {
                    log.warn("No bird found for name: {}", request.getBirdName());
                    return new BirdNotFoundException(request.getBirdName());
                });

        // Buscar avistamiento pendiente
        SightingEntity pendingSighting = sightingRepository.findById(request.getSightingId())
                .orElseThrow(() -> {
                    log.warn("No sighting found for ID: {}", request.getSightingId());
                    return new IllegalArgumentException("No se encontró el avistamiento con ID " + request.getSightingId());
                });

        if (!SightingStatus.PENDING.name().equals(pendingSighting.getState())) {
            log.warn("El avistamiento {} no está en estado PENDING (actual: {})",
                    request.getSightingId(), pendingSighting.getState());
            throw new IllegalStateException("El avistamiento no está pendiente de confirmación");
        }

        // Procesar coordenadas
        BigDecimal lat = request.getLatitude();
        BigDecimal lon = request.getLongitude();

        if (lat == null || lon == null) {
            BigDecimal[] parsed = tryParseLatLonFromLocation(request.getLocation());
            if (parsed != null) {
                lat = parsed[0];
                lon = parsed[1];
            }
        }

        if (lat == null || lon == null) {
            throw new IllegalArgumentException("latitude/longitude son obligatorios (o pasá location=\"lat,lon\")");
        }

        validateLatLon(lat, lon);

        // --- Actualizar y guardar avistamiento ---
        SightingEntity confirmedSighting = SightingEntity.builder()
                .sightingId(pendingSighting.getSightingId()) // conservamos el mismo ID
                .user(userEntity)
                .bird(birdEntity)
                .dateTime(defaultIfNull(request.getDateTime(), pendingSighting.getDateTime()))
                .latitude(lat)
                .longitude(lon)
                .locationText(request.getLocationText())
                .state(SightingStatus.CONFIRMED.name())
                .build();

        sightingRepository.save(confirmedSighting);
        log.info("✅ Avistamiento confirmado y actualizado con ID {}", confirmedSighting.getSightingId());

        // --- Puntos y misiones ---
        int pointsAdded = pointsService.addPointsForSighting(userEntity, birdEntity);
        log.info("Se sumaron {} puntos al usuario {} por avistamiento de {}",
                pointsAdded, userEntity.getEmail(), birdEntity.getName());

        missionService.checkAndUpdateMissions(userEntity, birdEntity, confirmedSighting);
        achievementService.checkAndUpdateAchievements(userEntity, birdEntity, confirmedSighting);

        log.info("🎯 Misiones y logros actualizados para el avistamiento {}", confirmedSighting.getSightingId());
    }


    public SightingImagesByEmailResponse getSightingImagesByUserAndBirdName(SightingImageRequest req) {
        String slugBird = Slugs.snake(req.getBirdName());

        UserEntity user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", req.getEmail());
            return new UserNotFoundException(req.getEmail());
        });

        BirdEntity bird = birdRepository.findByName(req.getBirdName()).orElseThrow(() -> {
            log.warn("No bird found for name: {}", req.getBirdName());
            return new UserNotFoundException(req.getBirdName());
        });

        List<UUID> sightningIDList = sightingRepository.findIdsByBirdIdAndUserId(bird.getBirdId(), user.getUserId());
        List<List<SightingImageItem>> sightingImageItems = new ArrayList<>();

        for (UUID uuid : sightningIDList) {
            String prefix = req.getEmail() + "/" + slugBird + "/" + uuid + "/"; // sin "sightings/" al inicio
            var items = bucketService.listSightingImageUrls(prefix, Integer.MAX_VALUE);

            sightingImageItems.add(items);
        }


        return SightingImagesByEmailResponse.builder()
                .images(sightingImageItems)
                .build();
    }

    // ==========================
    // Avistajes por usuario (DB)
    // ==========================
    public SightingByUserResponse getSightingsByUser(String email) {
        validateIfEmailExists(email);

        List<SightingEntity> entities = sightingRepository.findByUserEmailAndDeletedFalse(email);

        // Mapear uno a uno, resolviendo portada por sightingId
        List<SightingResponse> responses = entities.stream()
                .map(se -> toSummaryWithCover(se))
                .sorted(Comparator.comparing(SightingResponse::getDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return SightingByUserResponse.builder()
                .sightingResponseList(responses)
                .build();
    }

    /**
     * Construye el resumen y resuelve coverThumbUrl/coverImageUrl SOLO
     * desde el folder de este sighting: {email}/{slug(birdName)}/{sightingId}/
     */
    private SightingResponse toSummaryWithCover(SightingEntity se) {
        String email = se.getUser() != null ? se.getUser().getEmail() : null;
        String bName = se.getBird() != null ? se.getBird().getName() : null;          // canónico
        String cName = se.getBird() != null ? se.getBird().getCommonName() : null;    // común
        UUID sightingId = se.getSightingId();

        // Rareza
        String rarity = (bName != null)
                ? birdRarityRepository.findRarityNameByBirdName(bName).orElse("")
                : "";

        // Prefijo específico del avistaje
        String slugBird = Slugs.of(bName != null ? bName : "unknown");
        String prefix = (email != null ? email : "unknown")
                + "/" + slugBird
                + "/" + sightingId + "/";

        // Pedimos SOLO la primera media como “cover”
        List<SightingImageItem> media = bucketService.listSightingImageUrls(prefix, 1);

        String coverThumb = (media != null && !media.isEmpty()) ? media.get(0).getThumbUrl() : null;
        String coverImage = (media != null && !media.isEmpty()) ? media.get(0).getImageUrl() : null;

        return SightingResponse.builder()
                .birdName(bName)
                .commonName(cName)
                .rarity(rarity)
                .dateTime(se.getDateTime())
                .latitude(se.getLatitude())
                .longitude(se.getLongitude())
                .coverThumbUrl(coverThumb)
                .coverImageUrl(coverImage)
                .build();
    }

    // =============================================================
    // Avistajes “míos” y “de otros” para un ave (con URLs públicas)
    // =============================================================
    public SightingsForBirdResponse getSightingsMineAndOthers(String email, String birdName) {
        userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });

        BirdEntity bird = birdRepository.findFirstByNameContainingIgnoreCase(birdName).orElseThrow(() -> {
            log.warn("No bird found for name: {}", birdName);
            return new BirdNotFoundException(birdName);
        });

        String canonicalBirdName = bird.getName();
        String commonName = bird.getCommonName();

        String rarity = birdRarityRepository
                .findRarityNameByBirdName(canonicalBirdName)
                .orElse("");

        List<SightingEntity> mineEntities =
                sightingRepository.findByBird_NameIgnoreCaseAndUser_EmailAndDeletedFalseOrderByDateTimeDesc(
                        canonicalBirdName, email);

        List<SightingEntity> othersEntities =
                sightingRepository.findByBird_NameIgnoreCaseAndUser_EmailNotAndDeletedFalseOrderByDateTimeDesc(
                        canonicalBirdName, email);

        // Importante: ya NO cacheamos por email. Las fotos se buscan por sightingId.
        List<SightingFullResponse> mine = mineEntities.stream()
                .map(se -> toFullResponseOnlyThisSighting(se, canonicalBirdName, commonName, rarity))
                .toList();

        List<SightingFullResponse> others = othersEntities.stream()
                .map(se -> toFullResponseOnlyThisSighting(se, canonicalBirdName, commonName, rarity))
                .toList();

        return SightingsForBirdResponse.builder()
                .birdName(canonicalBirdName)
                .commonName(commonName)
                .rarity(rarity)
                .mine(mine)
                .others(others)
                .build();
    }

    /**
     * Construye el response de un único avistaje y SOLO sus imágenes.
     * Prefijo: <email>/<slug(birdName)>/<sightingId>/
     */
    private SightingFullResponse toFullResponseOnlyThisSighting(SightingEntity se,
                                                                String canonicalBirdName,
                                                                String commonName,
                                                                String rarity) {

        String uEmail = se.getUser() != null ? se.getUser().getEmail() : null;
        String uName = se.getUser() != null ? se.getUser().getUsername() : null;

        UUID sightingId = se.getSightingId();
        String slugBird = Slugs.of(canonicalBirdName); // mismo slug que usás al subir
        String prefix = (uEmail != null ? uEmail : "unknown")
                + "/" + slugBird
                + "/" + sightingId + "/";

        // Traer imágenes del BUCKET solo para ESTE avistaje
        List<SightingImageItem> raw = bucketService.listSightingImageUrls(prefix, 50);

        // Filtro defensivo por si el bucket devuelve de más
        String needle = "/" + sightingId + "/";
        List<SightingImageItem> imgs = raw == null ? List.of() : raw.stream()
                .filter(it -> contains(it.getThumbUrl(), needle) || contains(it.getImageUrl(), needle))
                .toList();

        String coverThumb = imgs.isEmpty() ? null : imgs.get(0).getThumbUrl();
        String coverImage = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();

        String locationString = formatLocation(se.getLatitude(), se.getLongitude(), se.getLocationText());

        return SightingFullResponse.builder()
                .sightingId(sightingId)
                .birdName(canonicalBirdName)
                .commonName(commonName)
                .rarity(rarity)
                .location(locationString)
                .dateTime(se.getDateTime())
                .userEmail(uEmail)
                .username(uName)
                .coverThumbUrl(coverThumb)
                .coverImageUrl(coverImage)
                .images(imgs)                 // ← ahora sólo del folder del sighting
                .latitude(se.getLatitude())
                .longitude(se.getLongitude())
                .build();
    }

    private static boolean contains(String url, String needle) {
        return url != null && url.contains(needle);
    }

    // ===================
    // Búsqueda paginada
    // ===================
    public Page<SightingResponse> searchSightings(String rarity, String color, String zone, String size,
                                                  int page, int sizePage) {
        Pageable pageable = PageRequest.of(page, sizePage, Sort.by(Sort.Direction.DESC, "dateTime"));

        String r = n(rarity);
        String c = n(color);
        String s = n(size);

        Page<SightingEntity> result = sightingRepository.searchSightings(r, c, s, pageable);

        List<SightingDto> dtos = SightingMapper.toDtoList(result.getContent());
        List<SightingResponse> responses = buildResponseList(dtos);
        return new PageImpl<>(responses, pageable, result.getTotalElements());
    }

    // ===================
    // Helpers privados
    // ===================
    private String n(String v) {
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private SightingFullResponse toFullResponse(SightingEntity se,
                                                String canonicalBirdName,
                                                String commonName,
                                                String rarity,
                                                Map<String, List<SightingImageItem>> imagesByUserCache) {

        String uEmail = se.getUser() != null ? se.getUser().getEmail() : null;
        String uName = se.getUser() != null ? se.getUser().getUsername() : null;

        // prefijo dentro del bucket sightings:
        String slugBird = Slugs.of(canonicalBirdName);
        String prefix = (uEmail != null ? uEmail : "unknown") + "/" + slugBird + "/";

        List<SightingImageItem> imgs = imagesByUserCache.computeIfAbsent(
                uEmail != null ? uEmail : "unknown",
                k -> bucketService.listSightingImageUrls(prefix, 50)
        );

        // cover = primera imagen disponible (si hay)
        String coverThumb = null;
        String coverImage = null;
        if (imgs != null && !imgs.isEmpty()) {
            coverThumb = imgs.get(0).getThumbUrl();
            coverImage = imgs.get(0).getImageUrl();
        }

        String locationString = formatLocation(se.getLatitude(), se.getLongitude(), se.getLocationText());

        return SightingFullResponse.builder()
                .sightingId(se.getSightingId())
                .birdName(canonicalBirdName)
                .commonName(commonName)
                .rarity(rarity)
                .location(locationString)
                .dateTime(se.getDateTime())
                .userEmail(uEmail)
                .username(uName)
                .coverThumbUrl(coverThumb)
                .coverImageUrl(coverImage)
                .images(imgs)
                .latitude(se.getLatitude())
                .longitude(se.getLongitude())
                .build();
    }

    private void validateIfEmailExists(String email) {
        userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });
    }

    private List<SightingResponse> buildResponseList(List<SightingDto> sightingDtos) {
        List<SightingResponse> list = new ArrayList<>();
        for (SightingDto dto : sightingDtos) {
            SightingResponse.SightingResponseBuilder builder = SightingResponse.builder()
                    .commonName(dto.birdCommonName())
                    .birdName(dto.birdName())
                    .dateTime(dto.dateTime())
                    .latitude(dto.latitude())
                    .longitude(dto.longitude());

            String rarity = birdRarityRepository.findRarityNameByBirdName(dto.birdName()).orElse("");
            builder.rarity(rarity);

            list.add(builder.build());
        }
        return list;
    }

    private void sortList(List<SightingResponse> sightingResponseList) {
        sightingResponseList.sort(
                Comparator.comparing(
                        SightingResponse::getDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );
    }

    private static <T> T defaultIfNull(T v, T def) {
        return v != null ? v : def;
    }

    private static BigDecimal[] tryParseLatLonFromLocation(String location) {
        if (location == null || location.isBlank()) return null;
        String cleaned = location.trim().replaceAll("\\s+", " ");
        String[] parts = cleaned.contains(",") ? cleaned.split(",") : cleaned.split(" ");
        if (parts.length < 2) return null;
        try {
            BigDecimal lat = new BigDecimal(parts[0].trim());
            BigDecimal lon = new BigDecimal(parts[1].trim());
            return new BigDecimal[]{lat, lon};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void validateLatLon(BigDecimal lat, BigDecimal lon) {
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude fuera de rango (-90..90)");
        }
        if (lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude fuera de rango (-180..180)");
        }
    }

    private static String formatLocation(BigDecimal lat, BigDecimal lon, String locationText) {
        String latLon = (lat != null && lon != null) ? lat + "," + lon : "";
        if (locationText != null && !locationText.isBlank()) {
            return latLon.isBlank() ? locationText : (latLon + " (" + locationText + ")");
        }
        return latLon;
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isBlank()) return "image";
        int i = path.lastIndexOf('/');
        return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
    }

    private static String toContentType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return "image/jpeg";
        return mimeType;
    }
}
