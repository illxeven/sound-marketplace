package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.PlaybackHistory;
import com.soundmarketplace.sound_marketplace.domain.TrackComment;

import java.util.List;

public interface EngagementService {
    void likeTrack(Integer userId, Integer trackId);
    void unlikeTrack(Integer userId, Integer trackId);
    long getTrackLikesCount(Integer trackId);
    long getTrackPlaysCount(Integer trackId);
    TrackComment addComment(Integer userId, Integer trackId, String commentText);
    TrackComment addComment(Integer userId, Integer trackId, String commentText, Integer parentCommentId);
    TrackComment updateComment(Integer userId, Integer commentId, String commentText);
    void deleteComment(Integer userId, Integer commentId);
    List<TrackComment> getTrackComments(Integer trackId);
    void repostTrack(Integer userId, Integer trackId);
    PlaybackHistory recordPlayback(Integer userId, Integer trackId);
    boolean recordPlayEvent(String clientIdentifier, Integer userId, Integer trackId);
    void clearPlayDedupeCache();
    List<PlaybackHistory> getUserPlaybackHistory(Integer userId);
}
