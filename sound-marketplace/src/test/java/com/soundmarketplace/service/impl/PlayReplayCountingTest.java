package com.soundmarketplace.service.impl;

import com.soundmarketplace.sound_marketplace.domain.PlaybackHistory;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Replay counting rules for EngagementServiceImpl.recordPlayEvent (no Spring context). */
class PlayReplayCountingTest {

    /** Clock the test can move forward. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-17T12:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private TrackRepository trackRepository;
    private PlaybackHistoryRepository playbackHistoryRepository;
    private MutableClock clock;
    private EngagementServiceImpl service;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class);
        playbackHistoryRepository = mock(PlaybackHistoryRepository.class);
        clock = new MutableClock();
        service = new EngagementServiceImpl(
                mock(TrackLikeRepository.class),
                mock(TrackCommentRepository.class),
                mock(RepostRepository.class),
                playbackHistoryRepository,
                mock(UserAccountRepository.class),
                trackRepository,
                mock(UserProfileRepository.class),
                clock);
        when(playbackHistoryRepository.save(any(PlaybackHistory.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void track(int id, Integer durationSeconds) {
        Track t = Track.builder().id(id).title("t" + id).durationSeconds(durationSeconds).build();
        when(trackRepository.findById(id)).thenReturn(Optional.of(t));
    }

    @Test
    @DisplayName("Auto-replay of a 3-minute track counts again after half its length (90 s)")
    void replayCountsAfterHalfTrack() {
        track(1, 180);
        assertThat(service.recordPlayEvent(null, 7, 1)).isTrue();

        clock.advance(Duration.ofSeconds(60));
        assertThat(service.recordPlayEvent(null, 7, 1)).as("inside 90 s gap").isFalse();

        clock.advance(Duration.ofSeconds(30)); // 90 s after the first counted play
        assertThat(service.recordPlayEvent(null, 7, 1)).as("real replay").isTrue();

        verify(trackRepository, times(2)).incrementPlayCount(1);
    }

    @Test
    @DisplayName("Short tracks use the 30 s minimum gap; double taps never double-count")
    void shortTrackUsesMinimumGap() {
        track(2, 20);
        assertThat(service.recordPlayEvent(null, 7, 2)).isTrue();
        assertThat(service.recordPlayEvent(null, 7, 2)).isFalse();
        clock.advance(Duration.ofSeconds(30));
        assertThat(service.recordPlayEvent(null, 7, 2)).isTrue();
    }

    @Test
    @DisplayName("Signed-out listeners are keyed by IP; different IPs count separately")
    void anonymousListenersKeyedByIp() {
        track(3, 200);
        assertThat(service.recordPlayEvent("198.51.100.1", null, 3)).isTrue();
        assertThat(service.recordPlayEvent("198.51.100.1", null, 3)).isFalse();
        assertThat(service.recordPlayEvent("198.51.100.2", null, 3)).isTrue();
    }

    @Test
    @DisplayName("Replay gap is clamped between 30 s and 30 min")
    void gapClamping() {
        assertThat(EngagementServiceImpl.replayGap(null)).isEqualTo(Duration.ofSeconds(30));
        assertThat(EngagementServiceImpl.replayGap(10)).isEqualTo(Duration.ofSeconds(30));
        assertThat(EngagementServiceImpl.replayGap(240)).isEqualTo(Duration.ofSeconds(120));
        assertThat(EngagementServiceImpl.replayGap(7200)).isEqualTo(Duration.ofMinutes(30));
    }
}
