package com.birdex.service;

import com.birdex.config.BucketProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

import java.net.URI;

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
        String endpoint = bucketProperties.getEndpoint();
        String bucket = bucketProperties.getBucket();

        String accessKey = bucketProperties.getAccessKey();
        String secretKey = bucketProperties.getSecretKey();
        String region = bucketProperties.getRegion();

        log.info("Inicializando MinIO S3Client -> endpoint={}, bucket={}", endpoint, bucket);

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

        ensureBucketExists(bucket);
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

    public String upload(String key, byte[] data, String contentType) {
        String bucket = bucketProperties.getBucket();

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(data)
            );

            String url = buildPublicUrl(key);
            log.info("Archivo subido a MinIO: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Error subiendo '{}' al bucket '{}': {}", key, bucket, e.getMessage(), e);
            throw new RuntimeException("No se pudo subir el archivo a MinIO", e);
        }
    }

    public List<String> listImagesAsBase64(String email, String birdName) {
        return listImagesAsBase64(email, birdName, Integer.MAX_VALUE);
    }

    public List<String> listImagesAsBase64(String email, String birdName, int maxItems) {
        String bucket = bucketProperties.getBucket();

        String safeBird = (birdName == null || birdName.isBlank())
                ? "unknown"
                : birdName.trim().replaceAll("\\s+", "_");

        String prefix = email + "/" + safeBird + "/";

        List<String> results = new ArrayList<>();

        try {
            ListObjectsV2Request req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Iterable pages = s3.listObjectsV2Paginator(req);

            outer:
            for (var page : pages) {
                for (S3Object obj : page.contents()) {
                    String key = obj.key();
                    if (!isImageKey(key)) continue;

                    GetObjectRequest getReq = GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build();

                    try (InputStream in = s3.getObject(getReq)) {
                        byte[] bytes = readAll(in);
                        String b64 = Base64.getEncoder().encodeToString(bytes);
                        results.add(b64);
                        if (results.size() >= maxItems) break outer;
                    }
                }
            }

            log.info("Se obtuvieron {} imágenes en Base64 para el prefijo {}", results.size(), prefix);
            return results;

        } catch (Exception e) {
            log.error("Error listando/leyendo imágenes bajo '{}': {}", prefix, e.getMessage(), e);
            throw new RuntimeException("No se pudieron obtener las imágenes en Base64 desde MinIO", e);
        }
    }

    private static boolean isImageKey(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
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


    public String buildPublicUrl(String key) {
        String endpoint = bucketProperties.getEndpoint();
        String bucket = bucketProperties.getBucket();

        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", base, bucket, key);
    }
}