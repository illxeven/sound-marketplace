package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.ArtistToolsSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArtistToolsSubscriptionRepository extends JpaRepository<ArtistToolsSubscription, Integer> {

    @Query("SELECT s FROM ArtistToolsSubscription s WHERE s.artist.id = :artistId")
    List<ArtistToolsSubscription> findByArtistId(@Param("artistId") Integer artistId);
}
