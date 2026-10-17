package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.TrackComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackCommentRepository extends JpaRepository<TrackComment, Integer> {

    @Query("SELECT tc FROM TrackComment tc WHERE tc.track.id = :trackId ORDER BY tc.createdAt DESC")
    List<TrackComment> findByTrackIdOrderByCreatedAtDesc(@Param("trackId") Integer trackId);
}
