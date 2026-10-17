package com.soundmarketplace.controller;

import com.soundmarketplace.service.CatalogService;
import com.soundmarketplace.sound_marketplace.dto.AlbumDTO;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<ApiResponse<AlbumDTO>> createAlbum(@RequestBody CreateAlbumRequest request) {
        if (request.getArtistId() == null) {
            request.setArtistId(CurrentUser.getUserId());
        }
        AlbumDTO album = catalogService.createAlbum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Album created successfully", album));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AlbumDTO>> createAlbumWithCover(
            @RequestParam("title") String title,
            @RequestParam(value = "artistId", required = false) Integer artistId,
            @RequestParam(value = "coverArt", required = false) MultipartFile coverArt) {
        CreateAlbumRequest request = CreateAlbumRequest.builder()
                .title(title)
                .artistId(artistId != null ? artistId : CurrentUser.getUserId())
                .build();
        AlbumDTO album = catalogService.createAlbum(coverArt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Album created with cover successfully", album));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getAlbumCover(@PathVariable Integer id) {
        byte[] imageBytes = catalogService.getAlbumCoverImage(id);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AlbumDTO>> uploadAlbumCover(
            @PathVariable Integer id,
            @RequestParam("coverArt") MultipartFile coverArt) {
        AlbumDTO album = catalogService.updateAlbumCover(id, coverArt);
        return ResponseEntity.ok(ApiResponse.ok("Album cover updated", album));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlbumDTO>> updateAlbum(
            @PathVariable Integer id,
            @RequestBody CreateAlbumRequest request) {
        AlbumDTO album = catalogService.updateAlbum(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Album updated successfully", album));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlbum(@PathVariable Integer id) {
        catalogService.deleteAlbum(id);
        return ResponseEntity.ok(ApiResponse.ok("Album deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlbumDTO>>> getAllAlbums() {
        List<AlbumDTO> albums = catalogService.getAllAlbums();
        return ResponseEntity.ok(ApiResponse.ok(albums));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlbumDTO>> getAlbumById(@PathVariable Integer id) {
        AlbumDTO album = catalogService.getAlbumById(id);
        return ResponseEntity.ok(ApiResponse.ok(album));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<ApiResponse<List<AlbumDTO>>> getAlbumsByArtist(@PathVariable Integer artistId) {
        List<AlbumDTO> albums = catalogService.getAlbumsByArtist(artistId);
        return ResponseEntity.ok(ApiResponse.ok(albums));
    }
}
