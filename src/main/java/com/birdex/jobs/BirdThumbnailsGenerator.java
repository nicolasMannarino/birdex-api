package com.birdex.jobs;

import com.birdex.repository.BirdRepository;
import com.birdex.service.BucketService;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@ConditionalOnProperty(name = "birdex.thumbs.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class BirdThumbnailsGenerator implements CommandLineRunner {

    private final BirdRepository birdRepository;
    private final BucketService bucketService;

    private static final String CT_JPEG = "image/jpeg";
    private static final String CACHE = "public, max-age=31536000, immutable";

    @Override
    public void run(String... args) {
        var birds = birdRepository.findAllProjectedBy();
        int ok = 0, skipped = 0, missing = 0, errors = 0;

        log.info("=== Generando thumbnails JPEG para {} aves ===", birds.size());
        for (var b : birds) {
            String name = b.getName();
            String slug = Slugs.of(name);

            try {
                String key256 = slug + "/profile_256.jpg";
                String key600 = slug + "/profile_600.jpg";

                boolean has256 = bucketService.birdsObjectExists(key256);
                boolean has600 = bucketService.birdsObjectExists(key600);
                if (has256 && has600) {
                    skipped++;
                    continue;
                }

                String legacyKey = bucketService.resolveExistingProfileKey(name);
                byte[] src = bucketService.readBirdObject(legacyKey);
                if (src == null || src.length == 0) {
                    log.warn("Sin imagen fuente para {} (key: {})", name, legacyKey);
                    missing++;
                    continue;
                }

                BufferedImage img;
                try (var in = new ByteArrayInputStream(src)) {
                    img = ImageIO.read(in);
                }
                if (img == null) {
                    log.warn("No se pudo decodificar imagen de {}", legacyKey);
                    missing++;
                    continue;
                }

                if (!has256) {
                    byte[] out256 = resizeToJpeg(img, 256);
                    bucketService.uploadBirdObject(key256, out256, CT_JPEG, CACHE);
                }
                if (!has600) {
                    byte[] out600 = resizeToJpeg(img, 600);
                    bucketService.uploadBirdObject(key600, out600, CT_JPEG, CACHE);
                }

                ok++;
            } catch (Exception e) {
                errors++;
                log.error("Error generando thumbnails para '{}': {}", name, e.getMessage(), e);
            }
        }

        log.info("=== Thumbnails: ok={}, skipped(existing)={}, missingSource={}, errors={} ===",
                ok, skipped, missing, errors);
    }

    private static byte[] resizeToJpeg(BufferedImage src, int targetWidth) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        Thumbnails.of(src)
                .size(targetWidth, targetWidth)
                .outputFormat("jpg")
                .outputQuality(0.85f)
                .toOutputStream(baos);
        return baos.toByteArray();
    }
}
