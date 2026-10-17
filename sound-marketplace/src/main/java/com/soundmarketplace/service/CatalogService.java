package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.AlbumDTO;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
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
    Resource getTrackAudioResource(Integer trackId);
    TrackDTO getTrackById(Integer id);
    List<TrackDTO> getAllTracks();
    List<TrackDTO> getTracksByGenre(String genre);
    List<TrackDTO> getTracksByArtist(Integer artistId);

    AlbumDTO createAlbum(CreateAlbumRequest request);
    AlbumDTO createAlbum(MultipartFile coverArt, CreateAlbumRequest request);
    byte[] getAlbumCoverImage(Integer albumId);
    AlbumDTO updateAlbumCover(Integer albumId, MultipartFile coverArt);
    AlbumDTO getAlbumById(Integer id);
    List<AlbumDTO> getAllAlbums();
    List<AlbumDTO> getAlbumsByArtist(Integer artistId);
}
