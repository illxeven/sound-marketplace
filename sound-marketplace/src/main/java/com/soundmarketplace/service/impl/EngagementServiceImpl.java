package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.EngagementService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements EngagementService {

    private final TrackLikeRepository trackLikeRepository;
    private final TrackCommentRepository trackCommentRepository;
    private final RepostRepository repostRepository;
    private final PlaybackHistoryRepository playbackHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final TrackRepository trackRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void likeTrack(Integer userId, Integer trackId) {
        if (trackLikeRepository.findByUserIdAndTrackId(userId, trackId).isEmpty()) {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

            trackLikeRepository.save(TrackLike.builder()
                    .id(new TrackLikeId(userId, trackId))
                    .user(user)
                    .track(track)
                    .build());
        }
    }

    @Override
    @Transactional
    public void unlikeTrack(Integer userId, Integer trackId) {
        trackLikeRepository.findByUserIdAndTrackId(userId, trackId)
                .ifPresent(trackLikeRepository::delete);
    }

    @Override
    public long getTrackLikesCount(Integer trackId) {
        return trackLikeRepository.countByTrackId(trackId);
    }

    @Override
    public long getTrackPlaysCount(Integer trackId) {
        return playbackHistoryRepository.countByTrackId(trackId);
    }

    @Override
    @Transactional
    public TrackComment addComment(Integer userId, Integer trackId, String commentText) {
        return addComment(userId, trackId, commentText, null);
    }

    @Override
    @Transactional
    public TrackComment addComment(Integer userId, Integer trackId, String commentText, Integer parentCommentId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

        if (parentCommentId != null) {
            trackCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + parentCommentId));
        }

        String displayName = userProfileRepository.findByUserAccountId(userId)
                .map(UserProfile::getDisplayName)
                .filter(name -> !name.trim().isEmpty())
                .orElse(user.getEmail());

        return trackCommentRepository.save(TrackComment.builder()
                .user(user)
                .track(track)
                .commentText(commentText)
                .parentCommentId(parentCommentId)
                .userDisplayName(displayName)
                .build());
    }

    @Override
    @Transactional
    public TrackComment updateComment(Integer userId, Integer commentId, String commentText) {
        TrackComment comment = trackCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the author can edit this comment");
        }

        comment.setCommentText(commentText != null ? commentText.trim() : "");
        return trackCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Integer userId, Integer commentId) {
        TrackComment comment = trackCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the author can delete this comment");
        }

        trackCommentRepository.delete(comment);
    }

    @Override
    public List<TrackComment> getTrackComments(Integer trackId) {
        List<TrackComment> comments = trackCommentRepository.findByTrackIdOrderByCreatedAtDesc(trackId);
        for (TrackComment c : comments) {
            if (c.getUserDisplayName() == null || c.getUserDisplayName().trim().isEmpty()) {
                if (c.getUser() != null) {
                    String name = userProfileRepository.findByUserAccountId(c.getUser().getId())
                            .map(UserProfile::getDisplayName)
                            .filter(n -> !n.trim().isEmpty())
                            .orElse(c.getUser().getEmail());
                    c.setUserDisplayName(name);
                }
            }
        }
        return comments;
    }

    @Override
    @Transactional
    public void repostTrack(Integer userId, Integer trackId) {
        if (repostRepository.findByUserIdAndTrackId(userId, trackId).isEmpty()) {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

            repostRepository.save(Repost.builder().user(user).track(track).build());
        }
    }

    @Override
    @Transactional
    public PlaybackHistory recordPlayback(Integer userId, Integer trackId) {
        UserAccount user = (userId != null)
                ? userAccountRepository.findById(userId).orElse(null)
                : null;
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found: " + trackId));

        return playbackHistoryRepository.save(PlaybackHistory.builder()
                .user(user)
                .track(track)
                .build());
    }

    @Override
    public List<PlaybackHistory> getUserPlaybackHistory(Integer userId) {
        return playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }
}
