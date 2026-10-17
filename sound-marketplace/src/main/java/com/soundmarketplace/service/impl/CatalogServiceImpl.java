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
    private final com.soundmarketplace.storage.StorageService storageService;
    private final com.soundmarketplace.media.MediaValidationService mediaValidationService;
    private final com.soundmarketplace.media.DurationProbe durationProbe;
    private final com.soundmarketplace.media.MediaTranscoder mediaTranscoder;

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

        // Safe generation of internal file identifier without local filesystem ingestion (NTF-05)
        String cleanTitle = request.getTitle() != null
                ? request.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_")
                : "track";
        String resolvedFilePath = "/tracks/" + UUID.randomUUID() + "_" + cleanTitle + ".mp3";

        byte[] coverBlob = null;
        if (album != null && album.getCoverImageBlob() != null) {
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

        // 1. RBAC check on uploader roles (NTF-30)
        if (metadata.getUploaderUserId() != null) {
            UserAccount uploader = userAccountRepository.findById(metadata.getUploaderUserId()).orElse(null);
            if (uploader != null) {
                java.util.Set<String> roleNames = uploader.getRoles().stream()
                        .map(com.soundmarketplace.sound_marketplace.domain.Role::getRoleName)
                        .collect(Collectors.toSet());
                mediaValidationService.validateUploaderRoles(roleNames);
            }
        }

        // 2. Upload location eligibility
        userAccountService.validateTrackUploadEligibility(
                metadata.getUploaderUserId(),
                metadata.getArtistId(),
                metadata.getUploadSource()
        );

        // 3. MIME type from magic bytes & size validation (NTF-30)
        if (audioFile != null && !audioFile.isEmpty()) {
            mediaValidationService.validateAudioFile(audioFile);
        }
        if (videoFile != null && !videoFile.isEmpty()) {
            mediaValidationService.validateVideoFile(videoFile);
        }
        if (coverArt != null && !coverArt.isEmpty()) {
            mediaValidationService.validateImageFile(coverArt);
        }

        UserAccount artist = null;
        if (metadata.getArtistId() != null) {
            artist = userAccountRepository.findById(metadata.getArtistId())
                    .orElseThrow(() -> new IllegalArgumentException("Artist not found with id: " + metadata.getArtistId()));
        }

        Album album = null;
        if (metadata.getAlbumId() != null) {
            album = albumRepository.findById(metadata.getAlbumId())
                    .orElseThrow(() -> new IllegalArgumentException("Album not found with id: " + metadata.getAlbumId()));
        }

        String artistName = metadata.getArtistName();
        if (artistName == null || artistName.isBlank()) {
            artistName = artist != null ? artist.getEmail().split("@")[0] : "Unknown Artist";
        }

        String genreStr = metadata.getGenre() != null && !metadata.getGenre().trim().isEmpty()
                ? metadata.getGenre().trim() : "Other";
        Genre genre = genreRepository.findByNameIgnoreCase(genreStr)
                .orElseGet(() -> genreRepository.save(Genre.builder().name(genreStr).build()));

        // 4. Probe duration from audio via ffprobe (ignore client-supplied durationSeconds)
        Integer probedDuration = null;
        java.io.File tempAudioFile = null;
        try {
            if (audioFile != null && !audioFile.isEmpty()) {
                tempAudioFile = java.io.File.createTempFile("upload_probe_", "_" + audioFile.getOriginalFilename());
                audioFile.transferTo(tempAudioFile);
                probedDuration = durationProbe.probeDurationSeconds(tempAudioFile);
            }
        } catch (Exception e) {
            log.warn("Duration probe failed for uploaded audio: {}", e.getMessage());
        }

        String mediaType = (videoFile != null && !videoFile.isEmpty()) ? "VIDEO" : (metadata.getMediaType() != null ? metadata.getMediaType() : "AUDIO");

        // 5. Create track with status PROCESSING
        Track track = Track.builder()
                .title(metadata.getTitle() != null ? metadata.getTitle() : "Untitled")
                .artist(artist)
                .album(album)
                .genre(genre)
                .price(metadata.getPrice() != null ? metadata.getPrice() : BigDecimal.ZERO)
                .artistName(artistName)
                .features(metadata.getFeatures())
                .durationSeconds(probedDuration != null ? probedDuration : metadata.getDurationSeconds())
                .isPromoted(Boolean.TRUE.equals(metadata.getIsPromoted()))
                .mediaType(mediaType)
                .status("PROCESSING")
                .filePath("/tracks/processing_" + UUID.randomUUID())
                .build();

        track = trackRepository.save(track);

        try {
            // 6. Store original master file
            if (audioFile != null && !audioFile.isEmpty()) {
                String origName = audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio.mp3";
                String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.') + 1) : "mp3";
                String masterKey = "tracks/" + track.getId() + "/master." + ext;
                if (tempAudioFile != null && tempAudioFile.exists()) {
                    try (var fis = new java.io.FileInputStream(tempAudioFile)) {
                        storageService.store(masterKey, fis, tempAudioFile.length(), audioFile.getContentType());
                    }
                } else {
                    storageService.store(masterKey, audioFile.getInputStream(), audioFile.getSize(), audioFile.getContentType());
                }
                track.setMasterKey(masterKey);
                track.setFilePath(masterKey);
            }

            // 7. Store cover art
            if (coverArt != null && !coverArt.isEmpty()) {
                String origCover = coverArt.getOriginalFilename() != null ? coverArt.getOriginalFilename() : "cover.jpg";
                String ext = origCover.contains(".") ? origCover.substring(origCover.lastIndexOf('.') + 1) : "jpg";
                String coverKey = "tracks/" + track.getId() + "/cover." + ext;
                storageService.store(coverKey, coverArt.getInputStream(), coverArt.getSize(), coverArt.getContentType());
                track.setCoverKey(coverKey);
                track.setCoverImageBlob(coverArt.getBytes());
            }

            // 8. Transcoding
            if (tempAudioFile != null && tempAudioFile.exists()) {
                com.soundmarketplace.media.TranscodeResult result = mediaTranscoder.transcodeAudio(tempAudioFile, track.getId(), track.getDurationSeconds());
                if (result.success()) {
                    track.setStreamKey(result.streamKey());
                    track.setPreviewKey(result.previewKey());
                    track.setWaveformKey(result.waveformKey());
                    track.setStatus("READY");
                } else {
                    log.error("Transcoding failed for track {}: {}", track.getId(), result.errorMessage());
                    track.setStatus("FAILED");
                }
            } else {
                track.setStatus("READY");
            }

            if (videoFile != null && !videoFile.isEmpty()) {
                java.io.File tempVideoFile = java.io.File.createTempFile("video_upload_", "_" + videoFile.getOriginalFilename());
                try {
                    videoFile.transferTo(tempVideoFile);
                    com.soundmarketplace.media.TranscodeResult vResult = mediaTranscoder.transcodeVideo(tempVideoFile, track.getId());
                    if (vResult.success()) {
                        track.setStreamKey(vResult.streamKey());
                    }
                } finally {
                    tempVideoFile.delete();
                }
            }

            track = trackRepository.save(track);
            return mapTrackToDTO(track);
        } catch (Exception e) {
            log.error("Failed track upload post-processing: {}", e.getMessage(), e);
            track.setStatus("FAILED");
            trackRepository.save(track);
            throw new RuntimeException("Error processing track upload: " + e.getMessage(), e);
        } finally {
            if (tempAudioFile != null && tempAudioFile.exists()) {
                tempAudioFile.delete();
            }
        }
    }

    @Override
    public byte[] getTrackCoverImage(Integer trackId) {
        byte[] blob = trackRepository.findCoverImageBlobById(trackId);
        if (blob != null && blob.length > 0) {
            return blob;
        }
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track != null) {
            if (track.getCoverKey() != null && !track.getCoverKey().isBlank()) {
                byte[] bytes = storageService.loadBytes(track.getCoverKey());
                if (bytes != null && bytes.length > 0) {
                    return bytes;
                }
            }
            if (track.getAlbum() != null) {
                byte[] albBlob = albumRepository.findCoverImageBlobById(track.getAlbum().getId());
                if (albBlob != null && albBlob.length > 0) {
                    return albBlob;
                }
                if (track.getAlbum().getCoverKey() != null) {
                    return storageService.loadBytes(track.getAlbum().getCoverKey());
                }
            }
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
                String origCover = coverArt.getOriginalFilename() != null ? coverArt.getOriginalFilename() : "cover.jpg";
                String ext = origCover.contains(".") ? origCover.substring(origCover.lastIndexOf('.') + 1) : "jpg";
                String coverKey = "tracks/" + track.getId() + "/cover." + ext;
                storageService.store(coverKey, coverArt.getInputStream(), coverArt.getSize(), coverArt.getContentType());
                track.setCoverKey(coverKey);
                fileStorageService.storeFile(coverArt, "artwork");
            } catch (IOException e) {
                throw new RuntimeException("Could not read cover art file", e);
            }
        }
        Track saved = trackRepository.save(track);
        return mapTrackToDTO(saved);
    }

    @Override
    public TrackDTO getTrackById(Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + id));
        return mapTrackToDTO(track);
    }

    @Override
    public com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getAllTracks(org.springframework.data.domain.Pageable pageable) {
        return com.soundmarketplace.sound_marketplace.dto.PageResponse.from(trackRepository.findTrackSummaries(pageable));
    }

    @Override
    public List<TrackDTO> getAllTracks() {
        return trackRepository.findAll().stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getTracksByGenre(String genre, org.springframework.data.domain.Pageable pageable) {
        return com.soundmarketplace.sound_marketplace.dto.PageResponse.from(trackRepository.findTrackSummariesByGenre(genre, pageable));
    }

    @Override
    public List<TrackDTO> getTracksByGenre(String genre) {
        return trackRepository.findByGenreNameIgnoreCase(genre).stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getTracksByArtist(Integer artistId, org.springframework.data.domain.Pageable pageable) {
        return com.soundmarketplace.sound_marketplace.dto.PageResponse.from(trackRepository.findTrackSummariesByArtistId(artistId, pageable));
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
        if (album.getCoverImageBlob() != null && album.getCoverImageBlob().length > 0) {
            return album.getCoverImageBlob();
        }
        if (album.getCoverKey() != null && !album.getCoverKey().isBlank()) {
            return storageService.loadBytes(album.getCoverKey());
        }
        return null;
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
    public com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO> getAllAlbums(org.springframework.data.domain.Pageable pageable) {
        return com.soundmarketplace.sound_marketplace.dto.PageResponse.from(albumRepository.findAlbumSummaries(pageable));
    }

    @Override
    public List<AlbumDTO> getAllAlbums() {
        return albumRepository.findAll().stream()
                .map(this::mapAlbumToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO> getAlbumsByArtist(Integer artistId, org.springframework.data.domain.Pageable pageable) {
        return com.soundmarketplace.sound_marketplace.dto.PageResponse.from(albumRepository.findAlbumSummariesByArtistId(artistId, pageable));
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

        long likesCount = track.getLikeCount() != null ? track.getLikeCount().longValue() : 0L;
        long playsCount = track.getPlayCount() != null ? track.getPlayCount() : 0L;

        return TrackDTO.builder()
                .id(track.getId())
                .artistId(track.getArtist() != null ? track.getArtist().getId() : null)
                .artistName(track.getArtistName() != null ? track.getArtistName() :
                        (track.getArtist() != null ? track.getArtist().getEmail().split("@")[0] : null))
                .features(track.getFeatures())
                .albumId(track.getAlbum() != null ? track.getAlbum().getId() : null)
                .albumTitle(track.getAlbum() != null ? track.getAlbum().getTitle() : null)
                .title(track.getTitle())
                .genre(track.getGenre() != null ? track.getGenre().getName() : null)
                .durationSeconds(track.getDurationSeconds())
                .isPromoted(track.getIsPromoted())
                .price(track.getPrice())
                .hasCoverImage(hasCover)
                .coverImageUrl(coverUrl)
                .likesCount(likesCount)
                .playsCount(playsCount)
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
