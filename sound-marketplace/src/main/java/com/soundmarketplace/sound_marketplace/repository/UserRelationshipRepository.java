package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Integer> {

    @Query("SELECT ur FROM UserRelationship ur WHERE ur.follower.id = :followerId")
    List<UserRelationship> findByFollowerId(@Param("followerId") Integer followerId);

    @Query("SELECT ur FROM UserRelationship ur WHERE ur.followed.id = :followedId")
    List<UserRelationship> findByFollowedId(@Param("followedId") Integer followedId);

    long countByFollowedId(Integer followedId);
    long countByFollowerId(Integer followerId);
}
