package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

    @Query("SELECT p FROM Playlist p WHERE p.user.id = :userId")
    List<Playlist> findByUserId(@Param("userId") Integer userId);

    List<Playlist> findByIsPublicTrue();
}
