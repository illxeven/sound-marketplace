package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.AlbumDTO;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.dto.PageResponse;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CatalogService {
    TrackDTO createTrack(CreateTrackRequest request);
    TrackDTO uploadTrack(MultipartFile audioFile, MultipartFile coverArt, CreateTrackRequest metadata);
    TrackDTO uploadTrack(MultipartFile audioFile, MultipartFile coverArt, MultipartFile videoFile, CreateTrackRequest metadata);
    byte[] getTrackCoverImage(Integer trackId);
    TrackDTO updateTrackCover(Integer trackId, MultipartFile coverArt);
    TrackDTO getTrackById(Integer id);
    PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getAllTracks(org.springframework.data.domain.Pageable pageable);
    List<TrackDTO> getAllTracks();
    PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getTracksByGenre(String genre, org.springframework.data.domain.Pageable pageable);
    List<TrackDTO> getTracksByGenre(String genre);
    PageResponse<com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO> getTracksByArtist(Integer artistId, org.springframework.data.domain.Pageable pageable);
    List<TrackDTO> getTracksByArtist(Integer artistId);
    TrackDTO updateTrack(Integer id, CreateTrackRequest request);
    void deleteTrack(Integer id);

    AlbumDTO createAlbum(CreateAlbumRequest request);
    AlbumDTO createAlbum(MultipartFile coverArt, CreateAlbumRequest request);
    byte[] getAlbumCoverImage(Integer albumId);
    AlbumDTO updateAlbumCover(Integer albumId, MultipartFile coverArt);
    AlbumDTO getAlbumById(Integer id);
    PageResponse<com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO> getAllAlbums(org.springframework.data.domain.Pageable pageable);
    List<AlbumDTO> getAllAlbums();
    PageResponse<com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO> getAlbumsByArtist(Integer artistId, org.springframework.data.domain.Pageable pageable);
    List<AlbumDTO> getAlbumsByArtist(Integer artistId);
    AlbumDTO updateAlbum(Integer id, CreateAlbumRequest request);
    void deleteAlbum(Integer id);
}
