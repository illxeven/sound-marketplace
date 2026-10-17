package com.soundmarketplace.storage;

import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    String store(String key, InputStream inputStream, long contentLength, String contentType);

    default String store(String key, byte[] bytes, String contentType) {
        if (bytes == null) {
            return null;
        }
        return store(key, new ByteArrayInputStream(bytes), bytes.length, contentType);
    }

    Resource loadAsResource(String key);

    byte[] loadBytes(String key);

    void delete(String key);

    boolean exists(String key);

    String getPresignedUrl(String key, Duration duration);
}
