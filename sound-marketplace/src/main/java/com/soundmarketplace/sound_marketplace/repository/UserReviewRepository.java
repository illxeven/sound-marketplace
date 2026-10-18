package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReviewRepository extends JpaRepository<UserReview, Integer> {

    List<UserReview> findByTargetUserIdOrderByCreatedAtDesc(Integer targetUserId);

    List<UserReview> findByReviewerIdOrderByCreatedAtDesc(Integer reviewerId);

    /** One review per reviewer per artist (unique key uk_user_reviews_reviewer_target, V7). */
    java.util.Optional<UserReview> findByReviewerIdAndTargetUserId(Integer reviewerId, Integer targetUserId);

    @Query("SELECT AVG(r.rating) FROM UserReview r WHERE r.targetUser.id = :targetUserId")
    Double getAverageRatingByTargetUserId(@Param("targetUserId") Integer targetUserId);

    long countByTargetUserId(Integer targetUserId);
}
