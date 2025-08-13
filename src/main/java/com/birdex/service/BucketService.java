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

    public String buildPublicUrl(String key) {
        String endpoint = bucketProperties.getEndpoint();
        String bucket = bucketProperties.getBucket();

        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", base, bucket, key);
    }
}