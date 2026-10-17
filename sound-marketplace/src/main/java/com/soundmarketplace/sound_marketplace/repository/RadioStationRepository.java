package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.RadioStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RadioStationRepository extends JpaRepository<RadioStation, Integer> {

    @Query("SELECT r FROM RadioStation r WHERE r.user.id = :userId")
    List<RadioStation> findByUserId(@Param("userId") Integer userId);

    List<RadioStation> findBySeedGenreIgnoreCase(String seedGenre);
}
