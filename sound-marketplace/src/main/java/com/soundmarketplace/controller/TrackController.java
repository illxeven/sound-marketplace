package com.soundmarketplace.controller;

import com.soundmarketplace.service.CatalogService;
import com.soundmarketplace.service.EngagementService;
import com.soundmarketplace.sound_marketplace.domain.TrackComment;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import com.soundmarketplace.util.CurrentUser;
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
public class TrackController {

    private final CatalogService catalogService;
    private final EngagementService engagementService;
    private final com.soundmarketplace.service.PlaybackGateService playbackGateService;

    @GetMapping("/{id}/play")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse>> getPlayUrl(@PathVariable Integer id) {
        Integer userId = CurrentUser.getUserIdOrNull();
        com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse response = playbackGateService.getPlayUrl(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/play-urls")
    public ResponseEntity<ApiResponse<List<com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse>>> getPlayUrls(
            @RequestBody com.soundmarketplace.sound_marketplace.dto.PlayUrlsRequest request) {
        Integer userId = CurrentUser.getUserIdOrNull();
        List<com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse> responses =
                (request.getIds() == null ? List.<Integer>of() : request.getIds()).stream()
                        .map(id -> playbackGateService.getPlayUrl(id, userId))
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
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
            @RequestParam(value = "mediaType", required = false, defaultValue = "AUDIO") String mediaType) {

        Integer actingUserId = CurrentUser.getUserId();
        CreateTrackRequest request = CreateTrackRequest.builder()
                .title(title)
                .artistId(artistId != null ? artistId : actingUserId)
                .artistName(artistName)
                .features(features)
                .genre(genre)
                .price(price)
                .albumId(albumId)
                .durationSeconds(durationSeconds)
                .isPromoted(isPromoted)
                .uploadSource(uploadSource)
                .uploaderUserId(actingUserId)
                .mediaType(videoFile != null && !videoFile.isEmpty() ? "VIDEO" : mediaType)
                .build();

        TrackDTO track = catalogService.uploadTrack(audioFile, coverArt, videoFile, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Track uploaded successfully", track));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackDTO>> updateTrack(
            @PathVariable Integer id,
            @RequestBody CreateTrackRequest request) {
        TrackDTO track = catalogService.updateTrack(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Track updated successfully", track));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrack(@PathVariable Integer id) {
        catalogService.deleteTrack(id);
        return ResponseEntity.ok(ApiResponse.ok("Track deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO>>> getAllTracks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = com.soundmarketplace.util.PaginationUtil.createPageable(page, size, sort);
        var tracks = catalogService.getAllTracks(pageable);
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackDTO>> getTrackById(@PathVariable Integer id) {
        TrackDTO track = catalogService.getTrackById(id);
        return ResponseEntity.ok(ApiResponse.ok(track));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getTrackCover(
            @PathVariable Integer id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        byte[] imageBytes = catalogService.getTrackCoverImage(id);
        return com.soundmarketplace.util.ImageResponseUtil.createCacheableImageResponse(imageBytes, ifNoneMatch);
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TrackDTO>> uploadTrackCover(
            @PathVariable Integer id,
            @RequestParam("coverArt") MultipartFile coverArt) {
        TrackDTO track = catalogService.updateTrackCover(id, coverArt);
        return ResponseEntity.ok(ApiResponse.ok("Cover image updated", track));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO>>> getTracksByGenre(
            @PathVariable String genre,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = com.soundmarketplace.util.PaginationUtil.createPageable(page, size, sort);
        var tracks = catalogService.getTracksByGenre(genre, pageable);
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO>>> getTracksByArtist(
            @PathVariable Integer artistId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = com.soundmarketplace.util.PaginationUtil.createPageable(page, size, sort);
        var tracks = catalogService.getTracksByArtist(artistId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(tracks));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeTrack(@PathVariable Integer id) {
        Integer userId = CurrentUser.getUserId();
        engagementService.likeTrack(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Track liked", null));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeTrack(@PathVariable Integer id) {
        Integer userId = CurrentUser.getUserId();
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
            @RequestBody Map<String, Object> body) {
        Integer userId = CurrentUser.getUserId();
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
            @RequestBody Map<String, Object> body) {
        Integer userId = CurrentUser.getUserId();
        String commentText = body.get("commentText") != null ? body.get("commentText").toString() : "";
        // NTF-20: swap arguments in updateComment to (userId, commentId, commentText)
        TrackComment comment = engagementService.updateComment(userId, commentId, commentText);
        return ResponseEntity.ok(ApiResponse.ok("Comment updated", comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Integer commentId) {
        Integer userId = CurrentUser.getUserId();
        // NTF-20: swap arguments in deleteComment to (userId, commentId)
        engagementService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted", null));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TrackComment>>> getComments(@PathVariable Integer id) {
        List<TrackComment> comments = engagementService.getTrackComments(id);
        return ResponseEntity.ok(ApiResponse.ok(comments));
    }

    @PostMapping("/{id}/repost")
    public ResponseEntity<ApiResponse<Void>> repostTrack(@PathVariable Integer id) {
        Integer userId = CurrentUser.getUserId();
        engagementService.repostTrack(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Track reposted", null));
    }

    @PostMapping("/{id}/play-event")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> recordPlayEvent(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Request-Id", required = false) String headerRequestId,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            jakarta.servlet.http.HttpServletRequest request) {
        Integer userId = CurrentUser.getUserIdOrNull();
        // Signed-out listeners are keyed by IP. X-Request-Id is unique per request, so it can't
        // identify a listener (using it let anyone inflate play counts).
        String forwarded = request.getHeader("X-Forwarded-For");
        String clientIdentifier = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        boolean recorded = engagementService.recordPlayEvent(clientIdentifier, userId, id);
        long playCount = engagementService.getTrackPlaysCount(id);
        return ResponseEntity.ok(ApiResponse.ok("Play event processed", java.util.Map.of("recorded", recorded, "playCount", playCount)));
    }
}
