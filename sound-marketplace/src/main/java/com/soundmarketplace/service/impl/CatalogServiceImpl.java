package com.soundmarketplace.service.impl;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.CatalogService;
import com.soundmarketplace.service.FileStorageService;
import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.domain.Album;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.AlbumDTO;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import com.soundmarketplace.sound_marketplace.repository.AlbumRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.soundmarketplace.sound_marketplace.repository.GenreRepository;
import com.soundmarketplace.sound_marketplace.domain.Genre;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final UserAccountRepository userAccountRepository;
    private final FileStorageService fileStorageService;
    private final UserAccountService userAccountService;
    private final GenreRepository genreRepository;
    private final com.soundmarketplace.sound_marketplace.repository.TrackLikeRepository trackLikeRepository;
    private final com.soundmarketplace.sound_marketplace.repository.PlaybackHistoryRepository playbackHistoryRepository;

    @Override
    @Transactional
    public TrackDTO createTrack(CreateTrackRequest request) {
        // Enforce RBAC & Upload Location rules
        userAccountService.validateTrackUploadEligibility(
                request.getUploaderUserId(),
                request.getArtistId(),
                request.getUploadSource()
        );

        UserAccount artist = null;
        if (request.getArtistId() != null) {
            artist = userAccountRepository.findById(request.getArtistId())
                    .orElseThrow(() -> new IllegalArgumentException("Artist not found with id: " + request.getArtistId()));
        }

        Album album = null;
        if (request.getAlbumId() != null) {
            album = albumRepository.findById(request.getAlbumId())
                    .orElseThrow(() -> new IllegalArgumentException("Album not found with id: " + request.getAlbumId()));
        }

        String artistName = request.getArtistName();
        if (artistName == null || artistName.isBlank()) {
            if (artist != null) {
                artistName = artist.getEmail().split("@")[0];
            } else {
                artistName = "Unknown Artist";
            }
        }

        String resolvedFilePath = request.getFilePath();
        if (resolvedFilePath != null && !resolvedFilePath.isBlank()) {
            try {
                Path localFile = Paths.get(resolvedFilePath);
                if (Files.exists(localFile) && Files.isRegularFile(localFile)) {
                    String filename = localFile.getFileName().toString();
                    String storedName = UUID.randomUUID() + "_" + filename.replaceAll("[^a-zA-Z0-9.-]", "_");
                    Path targetDir = Paths.get("uploads", "tracks").toAbsolutePath().normalize();
                    Files.createDirectories(targetDir);
                    Path destination = targetDir.resolve(storedName);
                    Files.copy(localFile, destination, StandardCopyOption.REPLACE_EXISTING);
                    resolvedFilePath = "/tracks/" + storedName;
                    log.info("Ingested local audio file into backend storage: {}", resolvedFilePath);
                }
            } catch (Exception e) {
                log.debug("Path is not a local filesystem file: {}", e.getMessage());
            }
        } else {
            String cleanTitle = request.getTitle() != null
                    ? request.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_")
                    : "track";
            resolvedFilePath = "/tracks/" + UUID.randomUUID() + "_" + cleanTitle + ".mp3";
        }

        byte[] coverBlob = request.getCoverImageBlob();
        if (coverBlob == null && album != null && album.getCoverImageBlob() != null) {
            coverBlob = album.getCoverImageBlob();
        }

        String genreStr = request.getGenre();
        if (genreStr != null && genreStr.trim().isEmpty()) {
            genreStr = "Other";
        } else if (genreStr != null) {
            genreStr = genreStr.trim();
        } else {
            genreStr = "Other";
        }

        final String finalGenreStr = genreStr;
        Genre genreObj = genreRepository.findByNameIgnoreCase(finalGenreStr)
                .orElseGet(() -> genreRepository.save(Genre.builder().name(finalGenreStr).build()));

        String mediaType = (request.getMediaType() != null && !request.getMediaType().isBlank())
                ? request.getMediaType().toUpperCase()
                : "AUDIO";

        Track track = Track.builder()
                .artist(artist)
                .album(album)
                .title(request.getTitle())
                .filePath(resolvedFilePath)
                .genre(genreObj)
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .artistName(artistName)
                .features(request.getFeatures())
                .durationSeconds(request.getDurationSeconds())
                .isPromoted(request.getIsPromoted() != null ? request.getIsPromoted() : false)
                .coverImageBlob(coverBlob)
                .videoFilePath(request.getVideoFilePath())
                .videoBlob(request.getVideoBlob())
                .mediaType(mediaType)
                .build();

        Track saved = trackRepository.save(track);
        return mapTrackToDTO(saved);
    }

    @Override
    @Transactional
    public TrackDTO uploadTrack(MultipartFile audioFile, MultipartFile coverArt, CreateTrackRequest metadata) {
        return uploadTrack(audioFile, coverArt, null, metadata);
    }

    @Override
    @Transactional
    public TrackDTO uploadTrack(MultipartFile audioFile, MultipartFile coverArt, MultipartFile videoFile, CreateTrackRequest metadata) {
        if ((audioFile == null || audioFile.isEmpty()) && (videoFile == null || videoFile.isEmpty())) {
            throw new IllegalArgumentException("Either an audio file or a video file is required for track upload");
        }

        if (audioFile != null && !audioFile.isEmpty()) {
            String audioFilePath = fileStorageService.storeFile(audioFile, "tracks");
            metadata.setFilePath(audioFilePath);
        } else if (videoFile != null && !videoFile.isEmpty()) {
            String videoPath = fileStorageService.storeFile(videoFile, "videos");
            metadata.setFilePath(videoPath);
            metadata.setMediaType("VIDEO");
        }

        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                metadata.setVideoBlob(videoFile.getBytes());
                metadata.setVideoFilePath(fileStorageService.storeFile(videoFile, "videos"));
                metadata.setMediaType("VIDEO");
            } catch (IOException e) {
                log.warn("Failed to read video file bytes: {}", e.getMessage());
            }
        }

        if (coverArt != null && !coverArt.isEmpty()) {
            try {
                metadata.setCoverImageBlob(coverArt.getBytes());
            } catch (IOException e) {
                log.warn("Failed to read cover art bytes: {}", e.getMessage());
            }
            String artPath = fileStorageService.storeFile(coverArt, "artwork");
            log.info("Uploaded cover art saved at: {}", artPath);
        }

        return createTrack(metadata);
    }

    @Override
    public byte[] getTrackCoverImage(Integer trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found with id: " + trackId));
        if (track.getCoverImageBlob() != null && track.getCoverImageBlob().length > 0) {
            return track.getCoverImageBlob();
        }
        if (track.getAlbum() != null && track.getAlbum().getCoverImageBlob() != null) {
            return track.getAlbum().getCoverImageBlob();
        }
        return null;
    }

    @Override
    @Transactional
    public TrackDTO updateTrackCover(Integer trackId, MultipartFile coverArt) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + trackId));
        CurrentUser.requireOwnerOrAdmin(track.getArtist() != null ? track.getArtist().getId() : null);
        if (coverArt != null && !coverArt.isEmpty()) {
            try {
                track.setCoverImageBlob(coverArt.getBytes());
                fileStorageService.storeFile(coverArt, "artwork");
            } catch (IOException e) {
                throw new RuntimeException("Could not read cover art file", e);
            }
        }
        Track saved = trackRepository.save(track);
        return mapTrackToDTO(saved);
    }

    @Override
    public org.springframework.core.io.Resource getTrackAudioResource(Integer trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + trackId));
        if (track.getFilePath() == null || track.getFilePath().isBlank()) {
            return null;
        }
        return fileStorageService.loadAsResource(track.getFilePath());
    }

    @Override
    public TrackDTO getTrackById(Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + id));
        return mapTrackToDTO(track);
    }

    @Override
    public List<TrackDTO> getAllTracks() {
        return trackRepository.findAll().stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrackDTO> getTracksByGenre(String genre) {
        return trackRepository.findByGenreNameIgnoreCase(genre).stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrackDTO> getTracksByArtist(Integer artistId) {
        return trackRepository.findByArtistId(artistId).stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrackDTO updateTrack(Integer id, CreateTrackRequest request) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(track.getArtist() != null ? track.getArtist().getId() : null);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            track.setTitle(request.getTitle().trim());
        }
        if (request.getPrice() != null) {
            track.setPrice(request.getPrice());
        }
        if (request.getGenre() != null && !request.getGenre().isBlank()) {
            String gName = request.getGenre().trim();
            Genre genreObj = genreRepository.findByNameIgnoreCase(gName)
                    .orElseGet(() -> genreRepository.save(Genre.builder().name(gName).build()));
            track.setGenre(genreObj);
        }
        if (request.getFeatures() != null) {
            track.setFeatures(request.getFeatures());
        }
        if (request.getArtistName() != null) {
            track.setArtistName(request.getArtistName());
        }
        return mapTrackToDTO(trackRepository.save(track));
    }

    @Override
    @Transactional
    public void deleteTrack(Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(track.getArtist() != null ? track.getArtist().getId() : null);
        track.setDeletedAt(java.time.LocalDateTime.now());
        trackRepository.save(track);
    }

    @Override
    @Transactional
    public AlbumDTO createAlbum(CreateAlbumRequest request) {
        return createAlbum(null, request);
    }

    @Override
    @Transactional
    public AlbumDTO createAlbum(MultipartFile coverArt, CreateAlbumRequest request) {
        Integer artistId = request.getArtistId() != null ? request.getArtistId() : CurrentUser.getUserId();
        UserAccount artist = userAccountRepository.findById(artistId)
                .orElseThrow(() -> new NotFoundException("Artist not found with id: " + artistId));

        byte[] coverBlob = request.getCoverImageBlob();
        String artUrl = request.getCoverArtUrl();
        if (coverArt != null && !coverArt.isEmpty()) {
            try {
                coverBlob = coverArt.getBytes();
                artUrl = fileStorageService.storeFile(coverArt, "artwork");
            } catch (IOException e) {
                log.warn("Failed to read cover art file: {}", e.getMessage());
            }
        }

        Album album = Album.builder()
                .artist(artist)
                .title(request.getTitle())
                .coverArtUrl(artUrl)
                .coverImageBlob(coverBlob)
                .build();

        Album saved = albumRepository.save(album);
        return mapAlbumToDTO(saved);
    }

    @Override
    public byte[] getAlbumCoverImage(Integer albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + albumId));
        return album.getCoverImageBlob();
    }

    @Override
    @Transactional
    public AlbumDTO updateAlbumCover(Integer albumId, MultipartFile coverArt) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + albumId));
        CurrentUser.requireOwnerOrAdmin(album.getArtist() != null ? album.getArtist().getId() : null);
        if (coverArt != null && !coverArt.isEmpty()) {
            try {
                album.setCoverImageBlob(coverArt.getBytes());
                String path = fileStorageService.storeFile(coverArt, "artwork");
                album.setCoverArtUrl(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not read cover art", e);
            }
        }
        Album saved = albumRepository.save(album);
        return mapAlbumToDTO(saved);
    }

    @Override
    public AlbumDTO getAlbumById(Integer id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + id));
        return mapAlbumToDTO(album);
    }

    @Override
    public List<AlbumDTO> getAllAlbums() {
        return albumRepository.findAll().stream()
                .map(this::mapAlbumToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumDTO> getAlbumsByArtist(Integer artistId) {
        return albumRepository.findByArtistId(artistId).stream()
                .map(this::mapAlbumToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlbumDTO updateAlbum(Integer id, CreateAlbumRequest request) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(album.getArtist() != null ? album.getArtist().getId() : null);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            album.setTitle(request.getTitle().trim());
        }
        return mapAlbumToDTO(albumRepository.save(album));
    }

    @Override
    @Transactional
    public void deleteAlbum(Integer id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(album.getArtist() != null ? album.getArtist().getId() : null);
        album.setDeletedAt(java.time.LocalDateTime.now());
        albumRepository.save(album);
    }

    private TrackDTO mapTrackToDTO(Track track) {
        boolean hasCover = track.getCoverImageBlob() != null && track.getCoverImageBlob().length > 0;
        String coverUrl = hasCover
                ? "/api/tracks/" + track.getId() + "/cover"
                : (track.getAlbum() != null && track.getAlbum().getCoverArtUrl() != null ? track.getAlbum().getCoverArtUrl() : null);

        byte[] coverBlob = track.getCoverImageBlob() != null && track.getCoverImageBlob().length > 0
                ? track.getCoverImageBlob()
                : (track.getAlbum() != null ? track.getAlbum().getCoverImageBlob() : null);

        long likesCount = track.getId() != null ? trackLikeRepository.countByTrackId(track.getId()) : 0L;
        long playsCount = track.getId() != null ? playbackHistoryRepository.countByTrackId(track.getId()) : 0L;

        return TrackDTO.builder()
                .id(track.getId())
                .artistId(track.getArtist() != null ? track.getArtist().getId() : null)
                .artistName(track.getArtistName() != null ? track.getArtistName() :
                        (track.getArtist() != null ? track.getArtist().getEmail().split("@")[0] : null))
                .features(track.getFeatures())
                .albumId(track.getAlbum() != null ? track.getAlbum().getId() : null)
                .albumTitle(track.getAlbum() != null ? track.getAlbum().getTitle() : null)
                .title(track.getTitle())
                .filePath(track.getFilePath())
                .genre(track.getGenre() != null ? track.getGenre().getName() : null)
                .durationSeconds(track.getDurationSeconds())
                .isPromoted(track.getIsPromoted())
                .price(track.getPrice())
                .hasCoverImage(hasCover)
                .coverImageUrl(coverUrl)
                .coverImageBlob(coverBlob)
                .likesCount(likesCount)
                .playsCount(playsCount)
                .videoFilePath(track.getVideoFilePath())
                .videoBlob(track.getVideoBlob())
                .mediaType(track.getMediaType() != null ? track.getMediaType() : "AUDIO")
                .createdAt(track.getCreatedAt())
                .build();
    }

    private AlbumDTO mapAlbumToDTO(Album album) {
        boolean hasCover = album.getCoverImageBlob() != null && album.getCoverImageBlob().length > 0;
        String coverUrl = hasCover ? "/api/albums/" + album.getId() + "/cover" : album.getCoverArtUrl();

        return AlbumDTO.builder()
                .id(album.getId())
                .artistId(album.getArtist() != null ? album.getArtist().getId() : null)
                .title(album.getTitle())
                .coverArtUrl(coverUrl)
                .hasCoverImage(hasCover)
                .coverImageUrl(coverUrl)
                .createdAt(album.getCreatedAt())
                .build();
    }
}
