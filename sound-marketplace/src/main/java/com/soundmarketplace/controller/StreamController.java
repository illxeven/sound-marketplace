package com.soundmarketplace.controller;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.PlaybackGateService;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/stream")
@Slf4j
public class StreamController {

    private final PlaybackGateService playbackGateService;
    private final TrackRepository trackRepository;
    private final StorageService storageService;

    public StreamController(
            PlaybackGateService playbackGateService,
            TrackRepository trackRepository,
            StorageService storageService) {
        this.playbackGateService = playbackGateService;
        this.trackRepository = trackRepository;
        this.storageService = storageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> stream(
            @PathVariable Integer id,
            @RequestParam("v") String v,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        if (!playbackGateService.verifySignature(id, v, exp, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + id));

        if (track.getDeletedAt() != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String mediaKey;
        if ("preview".equalsIgnoreCase(v)) {
            mediaKey = track.getPreviewKey() != null ? track.getPreviewKey()
                    : (track.getStreamKey() != null ? track.getStreamKey()
                    : (track.getMasterKey() != null ? track.getMasterKey() : track.getFilePath()));
        } else {
            mediaKey = track.getStreamKey() != null ? track.getStreamKey()
                    : (track.getMasterKey() != null ? track.getMasterKey() : track.getFilePath());
        }

        byte[] allBytes = null;
        if (mediaKey != null && !mediaKey.isBlank()) {
            allBytes = storageService.loadBytes(mediaKey);
            if (allBytes == null) {
                // Check if it's on local disk path
                try {
                    String clean = mediaKey.startsWith("/") ? mediaKey.substring(1) : mediaKey;
                    Path p = Paths.get(clean);
                    if (!Files.exists(p)) {
                        p = Paths.get("uploads").resolve(clean);
                    }
                    if (Files.exists(p) && Files.isReadable(p)) {
                        allBytes = Files.readAllBytes(p);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (allBytes == null || allBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        long totalLength = allBytes.length;
        String contentType = determineContentType(mediaKey);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "private, max-age=300");
        headers.setContentType(MediaType.parseMediaType(contentType));

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeSpec = rangeHeader.substring(6).trim();
            long start = 0;
            long end = totalLength - 1;

            int dashIdx = rangeSpec.indexOf('-');
            if (dashIdx != -1) {
                String startPart = rangeSpec.substring(0, dashIdx).trim();
                String endPart = rangeSpec.substring(dashIdx + 1).trim();

                if (!startPart.isEmpty()) {
                    start = Long.parseLong(startPart);
                }
                if (!endPart.isEmpty()) {
                    end = Long.parseLong(endPart);
                }
            }

            if (start > end || start >= totalLength) {
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + totalLength);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .headers(headers)
                        .build();
            }

            end = Math.min(end, totalLength - 1);
            int rangeLength = (int) (end - start + 1);

            byte[] rangeBytes = new byte[rangeLength];
            System.arraycopy(allBytes, (int) start, rangeBytes, 0, rangeLength);

            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + totalLength);
            headers.setContentLength(rangeLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(rangeBytes);
        }

        headers.setContentLength(totalLength);
        return ResponseEntity.ok()
                .headers(headers)
                .body(allBytes);
    }

    private String determineContentType(String mediaKey) {
        if (mediaKey == null) return "application/octet-stream";
        String lower = mediaKey.toLowerCase();
        if (lower.endsWith(".m4a") || lower.endsWith(".aac")) {
            return "audio/mp4";
        } else if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lower.endsWith(".wav")) {
            return "audio/wav";
        } else if (lower.endsWith(".flac")) {
            return "audio/flac";
        } else if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        } else if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
