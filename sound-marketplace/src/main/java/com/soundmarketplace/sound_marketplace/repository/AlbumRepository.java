package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Album;
import com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO(" +
            "a.id, a.title, art.id, art.email, a.price, " +
            "CONCAT('/api/albums/', a.id, '/cover'), " +
            "(SELECT count(t) FROM Track t WHERE t.album = a AND t.deletedAt IS NULL), " +
            "a.createdAt) " +
            "FROM Album a " +
            "LEFT JOIN a.artist art",
            countQuery = "SELECT count(a) FROM Album a")
    Page<AlbumSummaryDTO> findAlbumSummaries(Pageable pageable);

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.AlbumSummaryDTO(" +
            "a.id, a.title, art.id, art.email, a.price, " +
            "CONCAT('/api/albums/', a.id, '/cover'), " +
            "(SELECT count(t) FROM Track t WHERE t.album = a AND t.deletedAt IS NULL), " +
            "a.createdAt) " +
            "FROM Album a " +
            "LEFT JOIN a.artist art " +
            "WHERE art.id = :artistId",
            countQuery = "SELECT count(a) FROM Album a WHERE a.artist.id = :artistId")
    Page<AlbumSummaryDTO> findAlbumSummariesByArtistId(@Param("artistId") Integer artistId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.artist.id = :artistId")
    List<Album> findByArtistId(@Param("artistId") Integer artistId);

    @Query("SELECT a.coverImageBlob FROM Album a WHERE a.id = :id")
    byte[] findCoverImageBlobById(@Param("id") Integer id);
}
