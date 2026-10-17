package com.soundmarketplace.controller;

import com.soundmarketplace.service.CatalogService;
import com.soundmarketplace.sound_marketplace.dto.AlbumDTO;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
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
@CrossOrigin(origins = "*")
public class AlbumController {

    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<ApiResponse<AlbumDTO>> createAlbum(@RequestBody CreateAlbumRequest request) {
        AlbumDTO album = catalogService.createAlbum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Album created successfully", album));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AlbumDTO>> createAlbumWithCover(
            @RequestParam("title") String title,
            @RequestParam("artistId") Integer artistId,
            @RequestParam(value = "coverArt", required = false) MultipartFile coverArt) {
        CreateAlbumRequest request = CreateAlbumRequest.builder()
                .title(title)
                .artistId(artistId)
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
