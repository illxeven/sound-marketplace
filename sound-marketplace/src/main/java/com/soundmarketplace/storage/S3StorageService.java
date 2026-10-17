package com.soundmarketplace.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@Slf4j
public class S3StorageService implements StorageService {

    private final String bucket;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(
            @Value("${app.storage.s3.bucket:sound-marketplace}") String bucket,
            @Value("${app.storage.s3.region:us-east-1}") String region,
            @Value("${app.storage.s3.endpoint:#{null}}") String endpoint) {
        this.bucket = bucket;

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            URI endpointUri = URI.create(endpoint);
            clientBuilder.endpointOverride(endpointUri).forcePathStyle(true);
            presignerBuilder.endpointOverride(endpointUri);
        }

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    @Override
    public String store(String key, InputStream inputStream, long contentLength, String contentType) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
            return key;
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.error("S3 store error for key {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("S3 store error: " + e.getMessage(), e);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=store durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public Resource loadAsResource(String key) {
        byte[] bytes = loadBytes(key);
        return bytes != null ? new ByteArrayResource(bytes) : null;
    }

    @Override
    public byte[] loadBytes(String key) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            return s3Client.getObject(getRequest, ResponseTransformer.toBytes()).asByteArray();
        } catch (NoSuchKeyException e) {
            outcome = "error";
            code = 404;
            return null;
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.error("S3 getObject error for key {}: {}", key, e.getMessage());
            return null;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=loadBytes durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public void delete(String key) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            DeleteObjectRequest delRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(delRequest);
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.warn("S3 delete error for key {}: {}", key, e.getMessage());
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=delete durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPresignedUrl(String key, Duration duration) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.error("S3 presign error for key {}: {}", key, e.getMessage());
            return null;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=presign durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }
}
