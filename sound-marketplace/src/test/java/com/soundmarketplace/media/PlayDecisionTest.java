package com.soundmarketplace.media;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.PlaybackGateService;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.PlayUrlResponse;
import com.soundmarketplace.sound_marketplace.repository.EntitlementRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlayDecisionTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    private PlaybackGateService playbackGateService;

    private UserAccount artist;
    private UserAccount buyer;
    private UserAccount otherUser;

    @BeforeEach
    void setUp() {
        playbackGateService = new PlaybackGateService(
                trackRepository,
                entitlementRepository,
                "test-stream-secret-key-for-unit-tests"
        );

        artist = new UserAccount();
        artist.setId(10);
        artist.setEmail("artist@test.com");

        buyer = new UserAccount();
        buyer.setId(20);
        buyer.setEmail("buyer@test.com");

        otherUser = new UserAccount();
        otherUser.setId(30);
        otherUser.setEmail("other@test.com");
    }

    @Test
    @DisplayName("Free track returns FULL stream for any caller or anonymous")
    void freeTrackReturnsFullStream() {
        Track track = new Track();
        track.setId(1);
        track.setTitle("Free Track");
        track.setPrice(BigDecimal.ZERO);
        track.setArtist(artist);

        when(trackRepository.findById(1)).thenReturn(Optional.of(track));

        PlayUrlResponse anonResponse = playbackGateService.getPlayUrl(1, null);
        assertThat(anonResponse.getIsPreview()).isFalse();
        assertThat(anonResponse.getUrl()).contains("v=full");

        PlayUrlResponse otherResponse = playbackGateService.getPlayUrl(1, otherUser.getId());
        assertThat(otherResponse.getIsPreview()).isFalse();
        assertThat(otherResponse.getUrl()).contains("v=full");
    }

    @Test
    @DisplayName("Paid track returns FULL stream when caller is the artist")
    void paidTrackArtistGetsFullStream() {
        Track track = new Track();
        track.setId(2);
        track.setTitle("Paid Track");
        track.setPrice(new BigDecimal("1.99"));
        track.setArtist(artist);

        when(trackRepository.findById(2)).thenReturn(Optional.of(track));

        PlayUrlResponse response = playbackGateService.getPlayUrl(2, artist.getId());
        assertThat(response.getIsPreview()).isFalse();
        assertThat(response.getUrl()).contains("v=full");
    }

    @Test
    @DisplayName("Paid track returns FULL stream when caller owns entitlement")
    void paidTrackEntitledBuyerGetsFullStream() {
        Track track = new Track();
        track.setId(3);
        track.setTitle("Paid Track");
        track.setPrice(new BigDecimal("2.99"));
        track.setArtist(artist);

        when(trackRepository.findById(3)).thenReturn(Optional.of(track));
        when(entitlementRepository.existsByUserIdAndTrackId(buyer.getId(), 3)).thenReturn(true);

        PlayUrlResponse response = playbackGateService.getPlayUrl(3, buyer.getId());
        assertThat(response.getIsPreview()).isFalse();
        assertThat(response.getUrl()).contains("v=full");
    }

    @Test
    @DisplayName("Paid track returns PREVIEW stream for unentitled caller or anonymous")
    void paidTrackUnentitledGetsPreview() {
        Track track = new Track();
        track.setId(4);
        track.setTitle("Paid Track");
        track.setPrice(new BigDecimal("1.99"));
        track.setArtist(artist);

        when(trackRepository.findById(4)).thenReturn(Optional.of(track));
        when(entitlementRepository.existsByUserIdAndTrackId(otherUser.getId(), 4)).thenReturn(false);

        PlayUrlResponse response = playbackGateService.getPlayUrl(4, otherUser.getId());
        assertThat(response.getIsPreview()).isTrue();
        assertThat(response.getUrl()).contains("v=preview");

        PlayUrlResponse anonResponse = playbackGateService.getPlayUrl(4, null);
        assertThat(anonResponse.getIsPreview()).isTrue();
        assertThat(anonResponse.getUrl()).contains("v=preview");
    }

    @Test
    @DisplayName("Deleted track throws NotFoundException (404)")
    void deletedTrackThrowsNotFound() {
        Track track = new Track();
        track.setId(5);
        track.setTitle("Deleted Track");
        track.setDeletedAt(LocalDateTime.now());

        when(trackRepository.findById(5)).thenReturn(Optional.of(track));

        assertThatThrownBy(() -> playbackGateService.getPlayUrl(5, otherUser.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
