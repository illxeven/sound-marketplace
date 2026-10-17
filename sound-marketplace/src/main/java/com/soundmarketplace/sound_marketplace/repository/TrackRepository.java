package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    @Query("SELECT t FROM Track t WHERE t.artist.id = :artistId")
    List<Track> findByArtistId(@Param("artistId") Integer artistId);

    @Query("SELECT t FROM Track t WHERE t.album.id = :albumId")
    List<Track> findByAlbumId(@Param("albumId") Integer albumId);

    List<Track> findByGenreNameIgnoreCase(String genreName);

    List<Track> findByTitleContainingIgnoreCase(String title);
}
