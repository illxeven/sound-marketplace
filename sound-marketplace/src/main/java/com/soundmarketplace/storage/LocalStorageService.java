package com.soundmarketplace.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-root:storage}") String localRoot) {
        this.rootLocation = Paths.get(localRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize local storage root location: {}", this.rootLocation, e);
        }
    }

    @Override
    public String store(String key, InputStream inputStream, long contentLength, String contentType) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            Path targetFile = resolveKey(key);
            Files.createDirectories(targetFile.getParent());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.error("Failed to store file for key {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("Storage error: " + e.getMessage(), e);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=store durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public Resource loadAsResource(String key) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            Path targetFile = resolveKey(key);
            if (!Files.exists(targetFile) || !Files.isReadable(targetFile)) {
                // Fallback check: maybe it was stored in old uploads directory
                Path fallback = Paths.get("uploads").toAbsolutePath().resolve(key).normalize();
                if (Files.exists(fallback) && Files.isReadable(fallback)) {
                    targetFile = fallback;
                } else {
                    outcome = "error";
                    code = 404;
                    return null;
                }
            }
            return new UrlResource(targetFile.toUri());
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.warn("Failed to load resource for key {}: {}", key, e.getMessage());
            return null;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=load durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public byte[] loadBytes(String key) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        int code = 200;
        try {
            Path targetFile = resolveKey(key);
            if (!Files.exists(targetFile) || !Files.isReadable(targetFile)) {
                Path fallback = Paths.get("uploads").toAbsolutePath().resolve(key).normalize();
                if (Files.exists(fallback) && Files.isReadable(fallback)) {
                    targetFile = fallback;
                } else {
                    outcome = "error";
                    code = 404;
                    return null;
                }
            }
            return Files.readAllBytes(targetFile);
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.warn("Failed to read bytes for key {}: {}", key, e.getMessage());
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
            Path targetFile = resolveKey(key);
            Files.deleteIfExists(targetFile);
        } catch (Exception e) {
            outcome = "error";
            code = 500;
            log.warn("Failed to delete file for key {}: {}", key, e.getMessage());
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=storage op=delete durationMs={} outcome={} code={}", durationMs, outcome, code);
        }
    }

    @Override
    public boolean exists(String key) {
        Path targetFile = resolveKey(key);
        return Files.exists(targetFile) && Files.isReadable(targetFile);
    }

    @Override
    public String getPresignedUrl(String key, Duration duration) {
        // Local storage does not use AWS S3 presigned URLs; signed streaming URLs are handled by Token/HMAC in service
        return null;
    }

    private Path resolveKey(String key) {
        String cleanKey = key != null && key.startsWith("/") ? key.substring(1) : (key != null ? key : "");
        return this.rootLocation.resolve(cleanKey).normalize();
    }
}
