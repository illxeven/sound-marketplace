package com.soundmarketplace.media;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

@Service
@Slf4j
public class MediaValidationService {

    private final Tika tika = new Tika();

    @Value("${app.upload.max-audio-size:104857600}")
    private long maxAudioSize; // 100MB

    @Value("${app.upload.max-video-size:524288000}")
    private long maxVideoSize; // 500MB

    @Value("${app.upload.max-image-size:10485760}")
    private long maxImageSize; // 10MB

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mpeg",
            "audio/x-wav",
            "audio/wav",
            "audio/vnd.wave",
            "audio/flac",
            "audio/x-flac",
            "audio/mp4",
            "audio/x-m4a",
            "audio/aac",
            "audio/ogg",
            "application/ogg"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4"
    );

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_UPLOAD_ROLES = Set.of(
            "music_artist",
            "producer",
            "admin",
            "super_admin"
    );

    public void validateUploaderRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only music artists, producers, and administrators may upload tracks");
        }
        boolean hasAllowed = roles.stream().anyMatch(r -> {
            String clean = r.toLowerCase().replace("role_", "").trim();
            return ALLOWED_UPLOAD_ROLES.contains(clean);
        });
        if (!hasAllowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only music artists, producers, and administrators may upload tracks");
        }
    }

    public void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > maxAudioSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Audio file exceeds maximum size limit of " + maxAudioSize + " bytes");
        }
        String mimeType = detectMimeType(file);
        if (!ALLOWED_AUDIO_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported audio type: " + mimeType);
        }
    }

    public void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > maxVideoSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Video file exceeds maximum size limit of " + maxVideoSize + " bytes");
        }
        String mimeType = detectMimeType(file);
        if (!ALLOWED_VIDEO_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported video type: " + mimeType);
        }
    }

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > maxImageSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image file exceeds maximum size limit of " + maxImageSize + " bytes");
        }
        String mimeType = detectMimeType(file);
        if (!ALLOWED_IMAGE_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image type: " + mimeType);
        }
    }

    private String detectMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return tika.detect(is, file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("Could not detect MIME type for {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unable to inspect file media type");
        }
    }
}
