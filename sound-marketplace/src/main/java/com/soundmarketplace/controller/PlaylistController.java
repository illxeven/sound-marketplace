package com.soundmarketplace.controller;

import com.soundmarketplace.service.PlaylistService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreatePlaylistRequest;
import com.soundmarketplace.sound_marketplace.dto.PlaylistDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistDTO>> createPlaylist(@RequestBody CreatePlaylistRequest request) {
        PlaylistDTO playlist = playlistService.createPlaylist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Playlist created successfully", playlist));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaylistDTO>>> getPublicPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getPublicPlaylists();
        return ResponseEntity.ok(ApiResponse.ok(playlists));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistDTO>> getPlaylistById(@PathVariable Integer id) {
        PlaylistDTO playlist = playlistService.getPlaylistById(id);
        return ResponseEntity.ok(ApiResponse.ok(playlist));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PlaylistDTO>>> getUserPlaylists(@PathVariable Integer userId) {
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(userId);
        return ResponseEntity.ok(ApiResponse.ok(playlists));
    }

    @PostMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<ApiResponse<PlaylistDTO>> addTrackToPlaylist(
            @PathVariable Integer id,
            @PathVariable Integer trackId) {
        PlaylistDTO playlist = playlistService.addTrackToPlaylist(id, trackId);
        return ResponseEntity.ok(ApiResponse.ok("Track added to playlist", playlist));
    }
}
