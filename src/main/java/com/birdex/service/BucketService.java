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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BucketService {

    private final BucketProperties bucketProperties;
    private S3Client s3;

    private static final String PROFILE_BASE_NAME = "profile";
    private static final String CACHE_IMMUTABLE = "public, max-age=31536000, immutable";

    public BucketService(BucketProperties bucketProperties) {
        this.bucketProperties = bucketProperties;
    }

    @PostConstruct
    void init() {
        String endpoint = bucketProperties.getEndpoint();
        String accessKey = bucketProperties.getAccessKey();
        String secretKey = bucketProperties.getSecretKey();
        String region = bucketProperties.getRegion();

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

        // Asegurar buckets configurados
        String birdsBucket = birdsBucket();
        if (birdsBucket != null && !birdsBucket.isBlank()) ensureBucketExists(birdsBucket);
        else log.warn("minio.birds.bucket no configurado.");

        String sightingsBucket = sightingsBucket();
        if (sightingsBucket != null && !sightingsBucket.isBlank()) ensureBucketExists(sightingsBucket);
        else log.warn("minio.sightings.bucket no configurado.");

        String usersBucket = usersBucket();
        if (usersBucket != null && !usersBucket.isBlank()) ensureBucketExists(usersBucket);
        else log.warn("minio.users.bucket no configurado.");
    }

    private String usersBucket() {
        return (bucketProperties.getUsers() != null)
                ? bucketProperties.getUsers().getBucket()
                : null;
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

    // =========================
    // USERS (fotos de perfil)
    // =========================

    /**
     * Sube la foto base64 de un usuario al bucket de users y retorna la key final (p.ej. users/{email}/profile.jpg).
     */
    public String uploadUserProfileBase64(String email, String base64, String contentType, boolean generateVariants) {
        String bucket = usersBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.users.bucket' vacío o nulo");
        }
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("Base64 vacío para foto de perfil");
        }

        // Tomamos snapshot de lo que existe ANTES de subir
        List<String> existing = listUserProfileKeys(bucket, email);

        ContentAndPayload cap = extractContentTypeAndPayload(base64, contentType);
        String ext = extFromContentType(cap.contentType);
        String baseKey = userProfileBaseKey(email);         // <email>/profile
        String keyOrig = baseKey + ext;                     // <email>/profile.jpg (o .png/.webp)
        String key256 = variantKey(keyOrig, "_256");
        String key600 = variantKey(keyOrig, "_600");

        byte[] bytes = Base64.getDecoder().decode(cap.payload.replaceAll("\\s", "").getBytes(StandardCharsets.UTF_8));

        // 1) Subimos el original
        putObject(bucket, keyOrig, bytes, cap.contentType, CACHE_IMMUTABLE);

        // 2) (Opcional) variantes
        if (generateVariants) {
            // Aquí iría el resize real; placeholder con el mismo buffer
            putObject(bucket, key256, bytes, cap.contentType, CACHE_IMMUTABLE);
            putObject(bucket, key600, bytes, cap.contentType, CACHE_IMMUTABLE);
        }

        // 3) Borramos lo anterior (si existía) EXCEPTO las keys recién subidas
        var keep = new java.util.HashSet<String>();
        keep.add(keyOrig);
        if (generateVariants) {
            keep.add(key256);
            keep.add(key600);
        }

        for (String oldKey : existing) {
            if (!keep.contains(oldKey) && objectExists(bucket, oldKey)) {
                try {
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(oldKey).build());
                    log.info("Eliminado perfil anterior '{}'", oldKey);
                } catch (Exception e) {
                    log.warn("No se pudo borrar '{}': {}", oldKey, e.getMessage());
                }
            }
        }

        log.info("Foto de perfil actualizada para {} -> '{}'", email, keyOrig);
        return keyOrig;
    }

    /**
     * Retorna URL pública de la foto de perfil del usuario para un size dado: "orig" | "256" | "600".
     */
    public String getUserProfilePublicUrl(String email, String size) {
        String bucket = usersBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.users.bucket' vacío o nulo");
        }
        String baseKeyJpg = userProfileBaseKey(email) + ".jpg";
        String baseKeyPng = userProfileBaseKey(email) + ".png";
        String baseKeyWebp = userProfileBaseKey(email) + ".webp";

        // Detectar cuál existe como original
        String origKey = pickFirstExisting(bucket, List.of(baseKeyJpg, baseKeyPng, baseKeyWebp));
        if (origKey == null) return null; // no hay foto

        if ("256".equals(size)) {
            String k = variantKey(origKey, "_256");
            return buildPublicUrl(bucket, objectExists(bucket, k) ? k : origKey);
        } else if ("600".equals(size)) {
            String k = variantKey(origKey, "_600");
            return buildPublicUrl(bucket, objectExists(bucket, k) ? k : origKey);
        }
        // default "orig"
        return buildPublicUrl(bucket, origKey);
    }

    /**
     * Borra todas las variantes de la foto del usuario.
     */
    public void deleteUserProfile(String email) {
        String bucket = usersBucket();
        if (bucket == null || bucket.isBlank()) return;

        List<String> keys = listUserProfileKeys(bucket, email);
        for (String k : keys) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(k).build());
            } catch (Exception e) {
                log.warn("No se pudo borrar '{}': {}", k, e.getMessage());
            }
        }
    }

    private List<String> listUserProfileKeys(String bucket, String email) {
        String prefix = userProfileBaseKey(email); // <email>/profile
        List<String> keys = new ArrayList<>();

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        for (var page : s3.listObjectsV2Paginator(req)) {
            for (S3Object obj : page.contents()) {
                String k = obj.key();
                if (k != null && !k.endsWith("/")) {
                    keys.add(k);
                }
            }
        }
        return keys;
    }

    private String userProfileBaseKey(String email) {
        String safeEmail = email == null ? "unknown" : email.trim();
        return safeEmail + "/" + PROFILE_BASE_NAME;
    }

    private ContentAndPayload extractContentTypeAndPayload(String base64, String fallbackContentType) {
        // data:<mime>;base64,<payload>
        Pattern p = Pattern.compile("^data:([\\w/+-\\.]+);base64,(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(base64);
        if (m.find()) {
            String ct = m.group(1);
            String payload = m.group(2);
            return new ContentAndPayload(ct, payload);
        }
        String ct = (fallbackContentType != null && !fallbackContentType.isBlank()) ? fallbackContentType : "image/jpeg";
        return new ContentAndPayload(ct, base64);
    }

    private record ContentAndPayload(String contentType, String payload) {
    }

    private void putObject(String bucket, String key, byte[] data, String contentType, String cacheControl) {
        PutObjectRequest.Builder pb = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType);
        if (cacheControl != null && !cacheControl.isBlank()) {
            pb.cacheControl(cacheControl);
        }
        s3.putObject(pb.build(), RequestBody.fromBytes(data));
        log.info("Subido '{}' a bucket '{}' ({} bytes)", key, bucket, data.length);
    }

    private String pickFirstExisting(String bucket, List<String> keys) {
        for (String k : keys) {
            if (objectExists(bucket, k)) return k;
        }
        return null;
    }

    // =========================
    // BIRDS (perfiles)
    // =========================
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
                keyToUse = preferredKey;
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

    // =========================
    // SIGHTINGS (avistajes)
    // =========================

    /**
     * Sube un objeto AL BUCKET DE SIGHTINGS. La clave es del tipo {email}/{slug-bird}/{archivo}.
     */
    public void uploadSightingObject(String key, byte[] data, String contentType, String cacheControl) {
        String bucket = sightingsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.sightings.bucket' vacío o nulo");
        }
        try {
            PutObjectRequest.Builder pb = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType);
            if (cacheControl != null && !cacheControl.isBlank()) {
                pb.cacheControl(cacheControl);
            }
            s3.putObject(pb.build(), RequestBody.fromBytes(data));
            log.info("Subido '{}' a bucket sightings '{}' ({} bytes)", key, bucket, data.length);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo subir sightings/" + key, e);
        }
    }

    /**
     * Devuelve URLs públicas (thumb e image) de los objetos (imágenes, videos y audio .mp3) de un prefijo.
     * - Imágenes: intenta variantes _256 / _600; si no existen usa el original.
     * - Videos/Audio(.mp3): thumb = poster si existe (<nombre>_poster.jpg o <nombre>.jpg); imageUrl = el propio archivo.
     */
    public List<SightingImageItem> listSightingImageUrls(String prefix, int maxItems) {
        String bucket = sightingsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.sightings.bucket' vacío o nulo");
        }

        class Item {
            final String key;
            final java.time.Instant lastModified;

            Item(String key, java.time.Instant lm) {
                this.key = key;
                this.lastModified = lm;
            }
        }

        List<Item> keys = new ArrayList<>();
        try {
            ListObjectsV2Request req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            for (var page : s3.listObjectsV2Paginator(req)) {
                for (S3Object obj : page.contents()) {
                    String key = obj.key();
                    if (isMediaKey(key)) {
                        keys.add(new Item(key, obj.lastModified()));
                    }
                }
            }

            // más recientes primero
            keys.sort((a, b) -> b.lastModified.compareTo(a.lastModified));

            List<SightingImageItem> out = new ArrayList<>();
            for (Item it : keys) {
                String key = it.key;

                if (isImageKey(key)) {
                    // IMAGEN
                    String thumbKey = variantKey(key, "_256");
                    String imageKey = variantKey(key, "_600");

                    String keyForThumb = objectExists(bucket, thumbKey) ? thumbKey : key;
                    String keyForImage = objectExists(bucket, imageKey) ? imageKey : key;

                    out.add(new SightingImageItem(
                            buildPublicUrl(bucket, keyForThumb),
                            buildPublicUrl(bucket, keyForImage)
                    ));
                } else if (isVideo(key) || isAudio(key)) {
                    // VIDEO o AUDIO (.mp3)
                    String posterKey = resolvePosterKeyIfAny(bucket, key);
                    String thumbUrl = posterKey != null ? buildPublicUrl(bucket, posterKey) : null;

                    out.add(new SightingImageItem(
                            thumbUrl != null ? thumbUrl : buildPublicUrl(bucket, key),
                            buildPublicUrl(bucket, key)
                    ));
                }

                if (out.size() >= maxItems) break;
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("No se pudieron listar media en sightings bajo '" + prefix + "'", e);
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

    // --- Helpers de tipo de archivo ---
    private static boolean isImageKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private static boolean isVideo(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".mkv");
    }

    private static boolean isAudio(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3"); // pedido específico
    }

    private static boolean isMediaKey(String key) {
        return isImageKey(key) || isVideo(key) || isAudio(key);
    }

    private static String variantKey(String key, String suffix) {
        if (key == null || key.isBlank()) return key;
        int dot = key.lastIndexOf('.');
        if (dot <= 0) return key + suffix;
        return key.substring(0, dot) + suffix + key.substring(dot);
    }

    private static String replaceExt(String key, String newExt) {
        int dot = key.lastIndexOf('.');
        if (dot <= 0) return key + newExt;
        return key.substring(0, dot) + newExt;
    }

    /**
     * Para videos/audio: intentamos 1) sufijo _poster.jpg  2) mismo nombre con .jpg
     */
    private String resolvePosterKeyIfAny(String bucket, String mediaKey) {
        String candidate1 = replaceExt(variantKey(mediaKey, "_poster"), ".jpg");
        if (objectExists(bucket, candidate1)) return candidate1;

        String candidate2 = replaceExt(mediaKey, ".jpg");
        if (objectExists(bucket, candidate2)) return candidate2;

        return null;
    }

    public String putBirdProfileBase64(String birdName, String base64, String contentType) {
        String bucket = birdsBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Config 'minio.birds.bucket' vacío o nulo");
        }

        String slug = slugify(birdName);
        String ext = extFromContentType(contentType);
        String key = slug + "/" + profileBaseName() + ext;

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
