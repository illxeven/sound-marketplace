package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Integer> {

    @Query("SELECT ph FROM PlaybackHistory ph WHERE ph.user.id = :userId ORDER BY ph.playedAt DESC")
    List<PlaybackHistory> findByUserIdOrderByPlayedAtDesc(@Param("userId") Integer userId);

    @Query("SELECT COUNT(ph) FROM PlaybackHistory ph WHERE ph.track.id = :trackId")
    long countByTrackId(@Param("trackId") Integer trackId);
}

