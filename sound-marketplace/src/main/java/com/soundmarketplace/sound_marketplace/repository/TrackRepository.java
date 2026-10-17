package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO(" +
            "t.id, t.title, a.id, COALESCE(t.artistName, a.email), alb.id, alb.title, " +
            "g.name, t.durationSeconds, t.price, t.playCount, t.likeCount, t.mediaType, " +
            "CONCAT('/api/tracks/', t.id, '/cover'), t.createdAt) " +
            "FROM Track t " +
            "LEFT JOIN t.artist a " +
            "LEFT JOIN t.album alb " +
            "LEFT JOIN t.genre g " +
            "WHERE t.deletedAt IS NULL",
            countQuery = "SELECT count(t) FROM Track t WHERE t.deletedAt IS NULL")
    Page<TrackSummaryDTO> findTrackSummaries(Pageable pageable);

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO(" +
            "t.id, t.title, a.id, COALESCE(t.artistName, a.email), alb.id, alb.title, " +
            "g.name, t.durationSeconds, t.price, t.playCount, t.likeCount, t.mediaType, " +
            "CONCAT('/api/tracks/', t.id, '/cover'), t.createdAt) " +
            "FROM Track t " +
            "LEFT JOIN t.artist a " +
            "LEFT JOIN t.album alb " +
            "LEFT JOIN t.genre g " +
            "WHERE t.deletedAt IS NULL AND LOWER(g.name) = LOWER(:genre)",
            countQuery = "SELECT count(t) FROM Track t LEFT JOIN t.genre g WHERE t.deletedAt IS NULL AND LOWER(g.name) = LOWER(:genre)")
    Page<TrackSummaryDTO> findTrackSummariesByGenre(@Param("genre") String genre, Pageable pageable);

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.TrackSummaryDTO(" +
            "t.id, t.title, a.id, COALESCE(t.artistName, a.email), alb.id, alb.title, " +
            "g.name, t.durationSeconds, t.price, t.playCount, t.likeCount, t.mediaType, " +
            "CONCAT('/api/tracks/', t.id, '/cover'), t.createdAt) " +
            "FROM Track t " +
            "LEFT JOIN t.artist a " +
            "LEFT JOIN t.album alb " +
            "LEFT JOIN t.genre g " +
            "WHERE t.deletedAt IS NULL AND a.id = :artistId",
            countQuery = "SELECT count(t) FROM Track t WHERE t.deletedAt IS NULL AND t.artist.id = :artistId")
    Page<TrackSummaryDTO> findTrackSummariesByArtistId(@Param("artistId") Integer artistId, Pageable pageable);

    @Query("SELECT t FROM Track t WHERE t.artist.id = :artistId AND t.deletedAt IS NULL")
    List<Track> findByArtistId(@Param("artistId") Integer artistId);

    @Query("SELECT t FROM Track t WHERE t.album.id = :albumId AND t.deletedAt IS NULL")
    List<Track> findByAlbumId(@Param("albumId") Integer albumId);

    @Query("SELECT t FROM Track t WHERE t.album.id = :albumId AND t.deletedAt IS NULL")
    List<Track> findByAlbumIdAndDeletedAtIsNull(@Param("albumId") Integer albumId);

    List<Track> findByTitleContainingIgnoreCase(String title);

    List<Track> findByGenreNameIgnoreCase(String genre);

    @Query("SELECT t.coverImageBlob FROM Track t WHERE t.id = :id")
    byte[] findCoverImageBlobById(@Param("id") Integer id);

    @Query("SELECT t.videoBlob FROM Track t WHERE t.id = :id")
    byte[] findVideoBlobById(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Track t SET t.likeCount = t.likeCount + 1 WHERE t.id = :id")
    void incrementLikeCount(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Track t SET t.likeCount = CASE WHEN t.likeCount > 0 THEN t.likeCount - 1 ELSE 0 END WHERE t.id = :id")
    void decrementLikeCount(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Track t SET t.playCount = t.playCount + 1 WHERE t.id = :id")
    void incrementPlayCount(@Param("id") Integer id);
}
