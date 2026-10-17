package com.soundmarketplace.service.impl;

import com.soundmarketplace.exception.NotFoundException;
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
import com.soundmarketplace.util.CurrentUser;
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
        Integer userId = request.getUserId() != null ? request.getUserId() : CurrentUser.getUserId();
        CurrentUser.requireOwnerOrAdmin(userId);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

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
                .orElseThrow(() -> new NotFoundException("Playlist not found with id: " + id));
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
                .orElseThrow(() -> new NotFoundException("Playlist not found with id: " + playlistId));
        CurrentUser.requireOwnerOrAdmin(playlist.getUser() != null ? playlist.getUser().getId() : null);

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + trackId));

        if (!playlist.getTracks().contains(track)) {
            playlist.getTracks().add(track);
            playlist = playlistRepository.save(playlist);
        }

        return mapToDTO(playlist);
    }

    @Override
    @Transactional
    public PlaylistDTO updatePlaylist(Integer id, CreatePlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Playlist not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(playlist.getUser() != null ? playlist.getUser().getId() : null);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            playlist.setTitle(request.getTitle().trim());
        }
        if (request.getIsPublic() != null) {
            playlist.setIsPublic(request.getIsPublic());
        }
        return mapToDTO(playlistRepository.save(playlist));
    }

    @Override
    @Transactional
    public void deletePlaylist(Integer id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Playlist not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(playlist.getUser() != null ? playlist.getUser().getId() : null);
        playlistRepository.delete(playlist);
    }

    private PlaylistDTO mapToDTO(Playlist playlist) {
        List<TrackDTO> trackDTOs = playlist.getTracks() != null ? playlist.getTracks().stream()
                .map(t -> TrackDTO.builder()
                        .id(t.getId())
                        .artistId(t.getArtist() != null ? t.getArtist().getId() : null)
                        .albumId(t.getAlbum() != null ? t.getAlbum().getId() : null)
                        .albumTitle(t.getAlbum() != null ? t.getAlbum().getTitle() : null)
                        .title(t.getTitle())
                        .genre(t.getGenre() != null ? t.getGenre().getName() : null)
                        .price(t.getPrice())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList()) : new ArrayList<TrackDTO>();

        return PlaylistDTO.builder()
                .id(playlist.getId())
                .userId(playlist.getUser() != null ? playlist.getUser().getId() : null)
                .title(playlist.getTitle())
                .isPublic(playlist.getIsPublic())
                .tracks(trackDTOs)
                .createdAt(playlist.getCreatedAt())
                .build();
    }
}
