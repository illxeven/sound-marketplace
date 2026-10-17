package com.soundmarketplace.controller;

import com.soundmarketplace.service.CatalogService;
import com.soundmarketplace.service.EngagementService;
import com.soundmarketplace.sound_marketplace.domain.PlaybackHistory;
import com.soundmarketplace.sound_marketplace.domain.TrackComment;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrackController {

    private final CatalogService catalogService;
    private final EngagementService engagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrackDTO>> createTrack(@RequestBody CreateTrackRequest request) {
        TrackDTO track = catalogService.createTrack(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Track created successfully", track));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrackDTO>> uploadTrack(
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            @RequestParam(value = "coverArt", required = false) MultipartFile coverArt,
            @RequestParam("title") String title,
            @RequestParam(value = "artistId", required = false) Integer artistId,
            @RequestParam(value = "artistName", required = false) String artistName,
            @RequestParam(value = "features", required = false) String features,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "albumId", required = false) Integer albumId,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "isPromoted", required = false) Boolean isPromoted,
            @RequestParam(value = "uploadSource", required = false, defaultValue = "PROFILE_PAGE") String uploadSource,
            @RequestParam(value = "uploaderUserId", required = false) Integer uploaderUserId,
            @RequestParam(value = "mediaType", required = false, defaultValue = "AUDIO") String mediaType) {

        CreateTrackRequest request = CreateTrackRequest.builder()
                .title(title)
                .artistId(artistId)
                .artistName(artistName)
                .features(features)
                .genre(genre)
                .price(price)
                .albumId(albumId)
                .durationSeconds(durationSeconds)
                .isPromoted(isPromoted)
                .uploadSource(uploadSource)
                .uploaderUserId(uploaderUserId != null ? uploaderUserId : artistId)
                .mediaType(videoFile != null && !videoFile.isEmpty() ? "VIDEO" : mediaType)
                .build();

        TrackDTO track = catalogService.uploadTrack(audioFile, coverArt, videoFile, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Track uploaded successfully", track));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrackDTO>>> getAllTracks() {
        List<TrackDTO> tracks = catalogService.getAllTracks();
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackDTO>> getTrackById(@PathVariable Integer id) {
        TrackDTO track = catalogService.getTrackById(id);
        return ResponseEntity.ok(ApiResponse.ok(track));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getTrackCover(@PathVariable Integer id) {
        byte[] imageBytes = catalogService.getTrackCoverImage(id);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @GetMapping(value = {"/{id}/stream", "/{id}/audio"})
    public ResponseEntity<?> streamTrackAudio(@PathVariable Integer id) {
        try {
            org.springframework.core.io.Resource audioResource = catalogService.getTrackAudioResource(id);
            if (audioResource == null || !audioResource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(ApiResponse.error("Audio track not found for id: " + id));
            }

            String filename = audioResource.getFilename();
            MediaType mediaType = MediaType.parseMediaType("audio/mpeg");
            if (filename != null) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".wav")) mediaType = MediaType.parseMediaType("audio/wav");
                else if (lower.endsWith(".ogg")) mediaType = MediaType.parseMediaType("audio/ogg");
                else if (lower.endsWith(".flac")) mediaType = MediaType.parseMediaType("audio/flac");
                else if (lower.endsWith(".m4a") || lower.endsWith(".aac")) mediaType = MediaType.parseMediaType("audio/mp4");
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (filename != null ? filename : "track_" + id) + "\"")
                    .header(org.springframework.http.HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(audioResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Error streaming audio: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrackDTO>> uploadTrackCover(
            @PathVariable Integer id,
            @RequestParam("coverArt") MultipartFile coverArt) {
        TrackDTO track = catalogService.updateTrackCover(id, coverArt);
        return ResponseEntity.ok(ApiResponse.ok("Cover image updated", track));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<ApiResponse<List<TrackDTO>>> getTracksByGenre(@PathVariable String genre) {
        List<TrackDTO> tracks = catalogService.getTracksByGenre(genre);
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<ApiResponse<List<TrackDTO>>> getTracksByArtist(@PathVariable Integer artistId) {
        List<TrackDTO> tracks = catalogService.getTracksByArtist(artistId);
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeTrack(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        engagementService.likeTrack(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Track liked", null));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeTrack(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        engagementService.unlikeTrack(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Track unliked", null));
    }

    @GetMapping("/{id}/likes/count")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable Integer id) {
        long count = engagementService.getTrackLikesCount(id);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @GetMapping("/{id}/plays/count")
    public ResponseEntity<ApiResponse<Long>> getPlaysCount(@PathVariable Integer id) {
        long count = engagementService.getTrackPlaysCount(id);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TrackComment>> addComment(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestBody Map<String, Object> body) {
        String commentText = body.get("commentText") != null ? body.get("commentText").toString() : "";
        Integer parentCommentId = null;
        if (body.get("parentCommentId") != null) {
            try {
                parentCommentId = Integer.valueOf(body.get("parentCommentId").toString());
            } catch (NumberFormatException ignored) {}
        }
        TrackComment comment = engagementService.addComment(userId, id, commentText, parentCommentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Comment added", comment));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<TrackComment>> updateComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId,
            @RequestBody Map<String, Object> body) {
        String commentText = body.get("commentText") != null ? body.get("commentText").toString() : "";
        TrackComment comment = engagementService.updateComment(commentId, userId, commentText);
        return ResponseEntity.ok(ApiResponse.ok("Comment updated", comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Integer commentId,
            @RequestParam Integer userId) {
        engagementService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted", null));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TrackComment>>> getComments(@PathVariable Integer id) {
        List<TrackComment> comments = engagementService.getTrackComments(id);
        return ResponseEntity.ok(ApiResponse.ok(comments));
    }

    @PostMapping("/{id}/repost")
    public ResponseEntity<ApiResponse<Void>> repostTrack(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        engagementService.repostTrack(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Track reposted", null));
    }

    @RequestMapping(value = {"/{id}/play", "/{id}/playback"}, method = RequestMethod.POST)
    public ResponseEntity<ApiResponse<PlaybackHistory>> recordPlay(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer userId) {
        PlaybackHistory history = engagementService.recordPlayback(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Playback recorded", history));
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<byte[]> streamVideo(@PathVariable Integer id) {
        TrackDTO track = catalogService.getTrackById(id);
        if (track.getVideoBlob() != null && track.getVideoBlob().length > 0) {
            return ResponseEntity.ok()
                    .header("Content-Type", "video/mp4")
                    .body(track.getVideoBlob());
        }
        return ResponseEntity.notFound().build();
    }
}

