package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserQueueItem;
import com.soundmarketplace.sound_marketplace.domain.UserQueueItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQueueItemRepository extends JpaRepository<UserQueueItem, UserQueueItemId> {
    List<UserQueueItem> findByIdUserIdOrderByIdPositionAsc(Integer userId);
    void deleteByIdUserId(Integer userId);
}
