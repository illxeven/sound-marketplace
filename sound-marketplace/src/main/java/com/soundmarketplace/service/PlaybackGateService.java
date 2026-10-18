package com.soundmarketplace.service;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse;
import com.soundmarketplace.sound_marketplace.repository.EntitlementRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
public class PlaybackGateService {

    private final TrackRepository trackRepository;
    private final EntitlementRepository entitlementRepository;
    private final String streamSecret;

    public PlaybackGateService(
            TrackRepository trackRepository,
            EntitlementRepository entitlementRepository,
            @Value("${stream.secret:}") String streamSecret) {
        this.trackRepository = trackRepository;
        this.entitlementRepository = entitlementRepository;
        if (streamSecret == null || streamSecret.isBlank()) {
            // Never fall back to a key written in source: anyone could forge full-stream URLs.
            // Production refuses to start without STREAM_SECRET (ProductionConfigValidator); in dev we
            // use a random per-process key, so signed URLs simply stop working after a restart.
            byte[] random = new byte[32];
            new java.security.SecureRandom().nextBytes(random);
            this.streamSecret = Base64.getEncoder().encodeToString(random);
            log.warn("STREAM_SECRET not set; using a random per-process stream signing key (dev only)");
        } else {
            this.streamSecret = streamSecret;
        }
    }

    /**
     * How long a signed stream URL stays valid. The player keeps issuing Range requests with the same
     * URL while a track plays (and when seeking), and queued tracks are signed ahead of time, so this
     * must comfortably exceed a listening session; 5 minutes broke long tracks mid-play.
     */
    static final long STREAM_URL_TTL_SECONDS = 2 * 60 * 60;

    public PlayUrlResponse getPlayUrl(Integer trackId, Integer callerUserId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + trackId));

        if (track.getDeletedAt() != null) {
            throw new NotFoundException("Track not found with id: " + trackId);
        }

        boolean isFull = isFullStreamAllowed(track, callerUserId);
        long expiresAt = Instant.now().getEpochSecond() + STREAM_URL_TTL_SECONDS;
        String v = isFull ? "full" : "preview";
        String sig = sign(trackId, v, expiresAt);
        String url = "/api/stream/" + trackId + "?v=" + v + "&exp=" + expiresAt + "&sig=" + sig;

        return PlayUrlResponse.builder()
                .trackId(trackId)
                .url(url)
                .expiresAt(expiresAt)
                .isPreview(!isFull)
                .build();
    }

    public boolean isFullStreamAllowed(Track track, Integer callerUserId) {
        if (track == null || track.getDeletedAt() != null) {
            return false;
        }

        // 1. Free track (price == 0 or null)
        if (track.getPrice() == null || track.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        if (callerUserId == null) {
            return false;
        }

        // 2. Caller is the artist who owns the track
        if (track.getArtist() != null && callerUserId.equals(track.getArtist().getId())) {
            return true;
        }

        // 3. Caller has purchased/entitled the track
        return entitlementRepository.existsByUserIdAndTrackId(callerUserId, track.getId());
    }

    public String sign(Integer trackId, String v, long expiresAt) {
        String data = trackId + ":" + v + ":" + expiresAt;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(streamSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing stream request", e);
        }
    }

    public boolean verifySignature(Integer trackId, String v, long exp, String sig) {
        if (sig == null || sig.isBlank()) {
            return false;
        }
        if (Instant.now().getEpochSecond() > exp) {
            log.warn("Stream signature expired for track {}", trackId);
            return false;
        }
        String expectedSig = sign(trackId, v, exp);
        return MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8)
        );
    }
}
