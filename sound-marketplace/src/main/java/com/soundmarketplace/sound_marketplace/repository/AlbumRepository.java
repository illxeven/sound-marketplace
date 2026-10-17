package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {

    @Query("SELECT a FROM Album a WHERE a.artist.id = :artistId")
    List<Album> findByArtistId(@Param("artistId") Integer artistId);
}
