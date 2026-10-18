package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.EngagementService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EngagementServiceImpl implements EngagementService {

    private final TrackLikeRepository trackLikeRepository;
    private final TrackCommentRepository trackCommentRepository;
    private final RepostRepository repostRepository;
    private final PlaybackHistoryRepository playbackHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final TrackRepository trackRepository;
    private final UserProfileRepository userProfileRepository;

    /** Minimum gap between two counted plays of the same track by the same listener. */
    static final java.time.Duration MIN_REPLAY_GAP = java.time.Duration.ofSeconds(30);
    /** Upper bound so very long tracks can still be re-counted within a listening session. */
    static final java.time.Duration MAX_REPLAY_GAP = java.time.Duration.ofMinutes(30);

    /** listenerKey:trackId -> when the last play was counted. */
    private final com.github.benmanes.caffeine.cache.Cache<String, java.time.Instant> lastCountedPlay =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(MAX_REPLAY_GAP)
                    .maximumSize(100_000)
                    .build();

    private final java.time.Clock clock;

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
            trackRepository.incrementLikeCount(trackId);
        }
    }

    @Override
    @Transactional
    public void unlikeTrack(Integer userId, Integer trackId) {
        trackLikeRepository.findByUserIdAndTrackId(userId, trackId)
                .ifPresent(tl -> {
                    trackLikeRepository.delete(tl);
                    trackRepository.decrementLikeCount(trackId);
                });
    }

    @Override
    public long getTrackLikesCount(Integer trackId) {
        return trackRepository.findById(trackId)
                .map(Track::getLikeCount)
                .map(Integer::longValue)
                .orElseGet(() -> trackLikeRepository.countByTrackId(trackId));
    }

    @Override
    public long getTrackPlaysCount(Integer trackId) {
        return trackRepository.findById(trackId)
                .map(Track::getPlayCount)
                .orElseGet(() -> playbackHistoryRepository.countByTrackId(trackId));
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
                .orElseThrow(() -> new com.soundmarketplace.exception.NotFoundException("Comment not found with id: " + commentId));

        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(comment.getUser().getId());

        comment.setCommentText(commentText != null ? commentText.trim() : "");
        return trackCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Integer userId, Integer commentId) {
        TrackComment comment = trackCommentRepository.findById(commentId)
                .orElseThrow(() -> new com.soundmarketplace.exception.NotFoundException("Comment not found with id: " + commentId));

        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(comment.getUser().getId());

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

        PlaybackHistory saved = playbackHistoryRepository.save(PlaybackHistory.builder()
                .user(user)
                .track(track)
                .build());
        trackRepository.incrementPlayCount(trackId);
        return saved;
    }

    /**
     * Counts one listen. Called by the player when a track starts, finishes or auto-replays.
     * A replay is counted again once enough time has passed for it to be a real re-listen:
     * half the track's length, at least {@link #MIN_REPLAY_GAP} and at most {@link #MAX_REPLAY_GAP}.
     * Rapid repeated calls (double taps, retries, scripts) inside that window are ignored.
     *
     * @param clientIdentifier listener key for signed-out users (client IP); ignored when userId is set
     * @return true if the play was counted
     */
    @Override
    @Transactional
    public boolean recordPlayEvent(String clientIdentifier, Integer userId, Integer trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new com.soundmarketplace.exception.NotFoundException("Track not found: " + trackId));

        String listener = userId != null ? "user:" + userId : "ip:" + (clientIdentifier != null ? clientIdentifier : "unknown");
        String key = listener + ":" + trackId;
        java.time.Duration gap = replayGap(track.getDurationSeconds());
        java.time.Instant now = clock.instant();

        // Atomic check-and-set so two simultaneous requests can't both count.
        boolean[] counted = {false};
        lastCountedPlay.asMap().compute(key, (k, last) -> {
            if (last == null || !now.isBefore(last.plus(gap))) {
                counted[0] = true;
                return now;
            }
            return last;
        });

        if (!counted[0]) {
            log.debug("EVENT play.ignored trackId={} listener={} reason=within_replay_gap gapSec={}",
                    trackId, userId != null ? "user" : "anon", gap.toSeconds());
            return false;
        }

        UserAccount user = (userId != null) ? userAccountRepository.findById(userId).orElse(null) : null;
        playbackHistoryRepository.save(PlaybackHistory.builder()
                .user(user)
                .track(track)
                .build());
        trackRepository.incrementPlayCount(trackId);
        log.info("EVENT play.counted trackId={} userId={} gapSec={}", trackId, userId, gap.toSeconds());
        return true;
    }

    static java.time.Duration replayGap(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return MIN_REPLAY_GAP;
        }
        java.time.Duration half = java.time.Duration.ofSeconds(durationSeconds / 2L);
        if (half.compareTo(MIN_REPLAY_GAP) < 0) return MIN_REPLAY_GAP;
        if (half.compareTo(MAX_REPLAY_GAP) > 0) return MAX_REPLAY_GAP;
        return half;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getLikedTrackIds(Integer userId) {
        List<Integer> ids = trackLikeRepository.findByUserId(userId).stream()
                .map(like -> like.getTrack().getId())
                .toList();
        log.debug("EVENT likes.ids_loaded userId={} count={}", userId, ids.size());
        return ids;
    }

    @Override
    public void clearPlayDedupeCache() {
        lastCountedPlay.invalidateAll();
    }

    @Override
    public List<PlaybackHistory> getUserPlaybackHistory(Integer userId) {
        return playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }
}
