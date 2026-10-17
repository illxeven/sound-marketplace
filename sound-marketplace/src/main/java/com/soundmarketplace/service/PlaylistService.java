package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.CreatePlaylistRequest;
import com.soundmarketplace.sound_marketplace.dto.PlaylistDTO;

import java.util.List;

public interface PlaylistService {
    PlaylistDTO createPlaylist(CreatePlaylistRequest request);
    PlaylistDTO getPlaylistById(Integer id);
    List<PlaylistDTO> getPublicPlaylists();
    List<PlaylistDTO> getUserPlaylists(Integer userId);
    PlaylistDTO addTrackToPlaylist(Integer playlistId, Integer trackId);
}
