package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Repost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepostRepository extends JpaRepository<Repost, Integer> {

    @Query("SELECT r FROM Repost r WHERE r.user.id = :userId")
    List<Repost> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT r FROM Repost r WHERE r.track.id = :trackId")
    List<Repost> findByTrackId(@Param("trackId") Integer trackId);

    @Query("SELECT r FROM Repost r WHERE r.user.id = :userId AND r.track.id = :trackId")
    Optional<Repost> findByUserIdAndTrackId(@Param("userId") Integer userId, @Param("trackId") Integer trackId);
}
