package com.birdex.service;

import com.birdex.config.BucketProperties;
import com.birdex.domain.SightingImageItem;
import com.birdex.dto.enums.BirdImageSize;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class BucketService {

    private final BucketProperties bucketProperties;
    private S3Client s3;

    public BucketService(BucketProperties bucketProperties) {
        this.bucketProperties = bucketProperties;
    }

    @PostConstruct
    void init() {
        String endpoint  = bucketProperties.getEndpoint();
        String accessKey = bucketProperties.getAccessKey();
        String secretKey = bucketProperties.getSecretKey();
        String region    = bucketProperties.getRegion();

        log.info("Inicializando MinIO S3Client -> endpoint={}", endpoint);

        this.s3 = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        // Asegurar buckets configurados (solo los declarados)
        String birdsBucket = birdsBucket();
        if (birdsBucket != null && !birdsBucket.isBlank()) {
            ensureBucketExists(birdsBucket);
        } else {
            log.warn("minio.birds.bucket no configurado.");
        }

        String sightingsBucket = sightingsBucket();
        if (sightingsBucket != null && !sightingsBucket.isBlank()) {
            ensureBucketExists(sightingsBucket);
        } else {
            log.warn("minio.sightings.bucket no configurado.");
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket '{}' ya existe.", bucket);
        } catch (NoSuchBucketException e) {
            log.warn("Bucket '{}' no existe. Creando…", bucket);
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket '{}' creado correctamente.", bucket);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Not Found")) {
                log.warn("Bucket '{}' no existe (404). Creando…", bucket);
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Bucket '{}' creado correctamente.", bucket);
            } else {
                throw e;
            }
        }
    }

    public String getBirdProfilePublicUrl(String birdName, BirdImageSize size) {
        String bucket = birdsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.birds.bucket' vacío o nulo");
        }

        String slug = slugify(birdName);
        String preferredKey = switch (size) {
            case THUMB_256 -> slug + "/profile_256.jpg";
            case MEDIUM_600 -> slug + "/profile_600.jpg";
        };

        String keyToUse;
        if (objectExists(bucket, preferredKey)) {
            keyToUse = preferredKey;
        } else {
            keyToUse = resolveProfileKey(bucket, birdName);
            if (!objectExists(bucket, keyToUse)) {
                log.warn("Imagen no encontrada para '{}': ni '{}' ni '{}'", birdName, preferredKey, keyToUse);
                keyToUse = preferredKey; // fallback consistente
            }
        }
        return buildPublicUrl(bucket, keyToUse);
    }

    public boolean birdsObjectExists(String key) {
        String bucket = birdsBucket();
        return objectExists(bucket, key);
    }

    public byte[] readBirdObject(String key) {
        String bucket = birdsBucket();
        try (ResponseInputStream<GetObjectResponse> in =
                     s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return readAll(in);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return null;
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo objeto birds/" + key, e);
        }
    }

    public void uploadBirdObject(String key, byte[] data, String contentType, String cacheControl) {
        String bucket = birdsBucket();
        try {
            PutObjectRequest.Builder pb = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType);
            if (cacheControl != null && !cacheControl.isBlank()) {
                pb.cacheControl(cacheControl);
            }
            s3.putObject(pb.build(), RequestBody.fromBytes(data));
            log.info("Subido '{}' a bucket '{}' ({} bytes)", key, bucket, data.length);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo subir birds/" + key, e);
        }
    }

    public String resolveExistingProfileKey(String birdName) {
        String bucket = birdsBucket();
        return resolveProfileKey(bucket, birdName);
    }

    public String getBirdProfileBase64(String birdName) {
        String bucket = birdsBucket();
        String key = resolveProfileKey(bucket, birdName);

        try (ResponseInputStream<GetObjectResponse> in =
                     s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            byte[] bytes = readAll(in);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchKeyException e) {
            log.info("No existe foto de perfil '{}'/'{}'", bucket, key);
            return "";
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
            if (e.statusCode() == 404 || "NoSuchKey".equals(code)) {
                log.info("No existe foto de perfil '{}'/'{}' ({} {})", bucket, key, e.statusCode(), code);
                return "";
            }
            log.error("Error S3 leyendo perfil '{}'/'{}': {}", bucket, key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener la imagen de perfil desde MinIO", e);
        } catch (Exception e) {
            log.error("Error leyendo perfil '{}'/'{}': {}", bucket, key, e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener la imagen de perfil desde MinIO", e);
        }
    }

    public String getBirdProfileDataUri(String birdName) {
        String bucket = birdsBucket();
        String key = resolveProfileKey(bucket, birdName);

        String contentType = "image/jpeg";
        try {
            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            if (head.contentType() != null && !head.contentType().isBlank()) {
                contentType = head.contentType();
            } else {
                contentType = mimeFromExt(extensionOf(key));
            }
        } catch (Exception ignore) {
            log.error("Ignore exception");
        }

        String b64 = getBirdProfileBase64(birdName);
        return "data:" + contentType + ";base64," + b64;
    }

    private String resolveProfileKey(String bucket, String birdName) {
        String slug = slugify(birdName);
        String base = profileBaseName();
        String prefix = slug + "/" + base;

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        ListObjectsV2Iterable pages = s3.listObjectsV2Paginator(req);
        for (var page : pages) {
            for (S3Object obj : page.contents()) {
                String key = obj.key();
                if (!key.endsWith("/")) {
                    return key;
                }
            }
        }
        return slug + "/" + base + ".jpg";
    }

    private String birdsBucket() {
        return (bucketProperties.getBirds() != null)
                ? bucketProperties.getBirds().getBucket()
                : null;
    }

    private String sightingsBucket() {
        return (bucketProperties.getSightings() != null)
                ? bucketProperties.getSightings().getBucket()
                : null;
    }

    private String profileBaseName() {
        return (bucketProperties.getBirds() != null && bucketProperties.getBirds().getProfileObjectName() != null)
                ? bucketProperties.getBirds().getProfileObjectName()
                : "profile";
    }

    public List<SightingImageItem> listSightingImageUrls(String prefix, int maxItems) {
        String bucket = sightingsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.sightings.bucket' vacío o nulo");
        }

        List<SightingImageItem> out = new ArrayList<>();
        try {
            ListObjectsV2Request req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            for (var page : s3.listObjectsV2Paginator(req)) {
                for (S3Object obj : page.contents()) {
                    String key = obj.key();
                    if (!isImageKey(key)) continue;

                    String thumbKey = variantKey(key, "_256");
                    String imageKey = variantKey(key, "_600");

                    String keyForThumb = objectExists(bucket, thumbKey) ? thumbKey : key;
                    String keyForImage = objectExists(bucket, imageKey) ? imageKey : key;

                    out.add(new SightingImageItem(
                            buildPublicUrl(bucket, keyForThumb),
                            buildPublicUrl(bucket, keyForImage)
                    ));

                    if (out.size() >= maxItems) return out;
                }
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("No se pudieron listar imágenes en sightings bajo '" + prefix + "'", e);
        }
    }

    // =========================
    // Utilitarios comunes
    // =========================
    private boolean objectExists(String bucket, String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    public String buildPublicUrl(String bucket, String key) {
        String endpoint = bucketProperties.getEndpoint();
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;

        // Encodear cada segmento del path (soporta @, espacios, etc.)
        String[] segments = key.split("/");
        StringBuilder enc = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) enc.append('/');
            enc.append(java.net.URLEncoder.encode(segments[i], java.nio.charset.StandardCharsets.UTF_8));
        }
        return String.format("%s/%s/%s", base, bucket, enc.toString());
    }

    private static String slugify(String input) {
        if (input == null) return "unknown";
        String n = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private static String extensionOf(String key) {
        int i = (key != null) ? key.lastIndexOf('.') : -1;
        return (i > 0 && i < key.length() - 1) ? key.substring(i) : ".jpg";
    }

    private static String mimeFromExt(String ext) {
        if (ext == null) return "image/jpeg";
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".tif", ".tiff" -> "image/tiff";
            default -> "image/jpeg";
        };
    }

    private static String extFromContentType(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/tiff" -> ".tiff";
            default -> ".jpg";
        };
    }

    private static boolean isImageKey(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private static String variantKey(String key, String suffix) {
        if (key == null || key.isBlank()) return key;
        int dot = key.lastIndexOf('.');
        if (dot <= 0) return key + suffix;
        return key.substring(0, dot) + suffix + key.substring(dot);
    }

    public String putBirdProfileBase64(String birdName, String base64, String contentType) {
        String bucket = birdsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.birds.bucket' vacío o nulo");
        }

        String slug = slugify(birdName);
        String ext  = extFromContentType(contentType);
        String key  = slug + "/" + profileBaseName() + ext;

        byte[] bytes = Base64.getDecoder().decode(
                (base64.startsWith("data:") ? base64.substring(base64.indexOf(',') + 1) : base64)
                        .replaceAll("\\s", "")
                        .getBytes(StandardCharsets.UTF_8)
        );

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType != null ? contentType : mimeFromExt(ext))
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            log.info("Perfil '{}' subido en bucket '{}' ({} bytes)", key, bucket, bytes.length);
            return key;
        } catch (Exception e) {
            log.error("Error subiendo perfil '{}' al bucket '{}': {}", key, bucket, e.getMessage(), e);
            throw new RuntimeException("No se pudo subir la imagen de perfil a MinIO", e);
        }
    }
}
