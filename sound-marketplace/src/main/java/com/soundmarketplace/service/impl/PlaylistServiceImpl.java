package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.PlaylistService;
import com.soundmarketplace.sound_marketplace.domain.Playlist;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreatePlaylistRequest;
import com.soundmarketplace.sound_marketplace.dto.PlaylistDTO;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import com.soundmarketplace.sound_marketplace.repository.PlaylistRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserAccountRepository userAccountRepository;
    private final TrackRepository trackRepository;

    @Override
    @Transactional
    public PlaylistDTO createPlaylist(CreatePlaylistRequest request) {
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        List<Track> tracks = new ArrayList<>();
        if (request.getTrackIds() != null && !request.getTrackIds().isEmpty()) {
            tracks = trackRepository.findAllById(request.getTrackIds());
        }

        Playlist playlist = Playlist.builder()
                .user(user)
                .title(request.getTitle())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .tracks(tracks)
                .build();

        Playlist saved = playlistRepository.save(playlist);
        return mapToDTO(saved);
    }

    @Override
    public PlaylistDTO getPlaylistById(Integer id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + id));
        return mapToDTO(playlist);
    }

    @Override
    public List<PlaylistDTO> getPublicPlaylists() {
        return playlistRepository.findByIsPublicTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlaylistDTO> getUserPlaylists(Integer userId) {
        return playlistRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PlaylistDTO addTrackToPlaylist(Integer playlistId, Integer trackId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with id: " + playlistId));
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found with id: " + trackId));

        if (!playlist.getTracks().contains(track)) {
            playlist.getTracks().add(track);
            playlist = playlistRepository.save(playlist);
        }

        return mapToDTO(playlist);
    }

    private PlaylistDTO mapToDTO(Playlist playlist) {
        List<TrackDTO> trackDTOs = playlist.getTracks() != null ? playlist.getTracks().stream()
                .map(t -> TrackDTO.builder()
                        .id(t.getId())
                        .artistId(t.getArtist() != null ? t.getArtist().getId() : null)
                        .artistEmail(t.getArtist() != null ? t.getArtist().getEmail() : null)
                        .albumId(t.getAlbum() != null ? t.getAlbum().getId() : null)
                        .albumTitle(t.getAlbum() != null ? t.getAlbum().getTitle() : null)
                        .title(t.getTitle())
                        .filePath(t.getFilePath())
                        .genre(t.getGenre() != null ? t.getGenre().getName() : null)
                        .price(t.getPrice())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        return PlaylistDTO.builder()
                .id(playlist.getId())
                .userId(playlist.getUser() != null ? playlist.getUser().getId() : null)
                .userEmail(playlist.getUser() != null ? playlist.getUser().getEmail() : null)
                .title(playlist.getTitle())
                .isPublic(playlist.getIsPublic())
                .tracks(trackDTOs)
                .createdAt(playlist.getCreatedAt())
                .build();
    }
}
