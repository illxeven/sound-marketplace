package com.soundmarketplace.util;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.MessageDigest;
import java.time.Duration;

public final class ImageResponseUtil {

    private ImageResponseUtil() {}

    public static ResponseEntity<byte[]> createCacheableImageResponse(byte[] imageBytes, String ifNoneMatch) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = detectMediaType(imageBytes);
        String etag = "\"" + computeMD5(imageBytes) + "\"";

        if (ifNoneMatch != null) {
            String trimmedHeader = ifNoneMatch.trim();
            if (trimmedHeader.equals(etag) || trimmedHeader.equals("*") || trimmedHeader.equals(etag.replace("\"", ""))) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(604800)).cachePublic())
                        .build();
            }
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(604800)).cachePublic())
                .contentType(mediaType)
                .body(imageBytes);
    }

    public static MediaType detectMediaType(byte[] bytes) {
        if (bytes != null && bytes.length >= 4) {
            // PNG: 89 50 4E 47
            if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 &&
                (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
                return MediaType.IMAGE_PNG;
            }
            // JPEG: FF D8 FF
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF) {
                return MediaType.IMAGE_JPEG;
            }
            // WebP: RIFF ... WEBP (bytes 0-3 == 'RIFF', bytes 8-11 == 'WEBP')
            if (bytes.length >= 12 &&
                (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 &&
                (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46 &&
                (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45 &&
                (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
                return MediaType.parseMediaType("image/webp");
            }
            // GIF: 'GIF8'
            if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 &&
                (bytes[2] & 0xFF) == 0x46) {
                return MediaType.IMAGE_GIF;
            }
        }
        return MediaType.IMAGE_JPEG;
    }

    public static String computeMD5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 digest algorithm not found", e);
        }
    }
}
