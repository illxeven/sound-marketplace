package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.TrackLike;
import com.soundmarketplace.sound_marketplace.domain.TrackLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackLikeRepository extends JpaRepository<TrackLike, TrackLikeId> {

    @Query("SELECT tl FROM TrackLike tl WHERE tl.track.id = :trackId")
    List<TrackLike> findByTrackId(@Param("trackId") Integer trackId);

    @Query("SELECT tl FROM TrackLike tl WHERE tl.user.id = :userId")
    List<TrackLike> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT tl FROM TrackLike tl WHERE tl.user.id = :userId AND tl.track.id = :trackId")
    Optional<TrackLike> findByUserIdAndTrackId(@Param("userId") Integer userId, @Param("trackId") Integer trackId);

    @Query("SELECT COUNT(tl) FROM TrackLike tl WHERE tl.track.id = :trackId")
    long countByTrackId(@Param("trackId") Integer trackId);
}
