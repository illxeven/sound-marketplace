package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.Album;
import com.soundmarketplace.sound_marketplace.domain.Merchandise;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserProfile;
import com.soundmarketplace.sound_marketplace.repository.AlbumRepository;
import com.soundmarketplace.sound_marketplace.repository.MerchandiseRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserProfileRepository;
import com.soundmarketplace.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaExportService {

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final UserProfileRepository userProfileRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final StorageService storageService;

    @Transactional
    public Map<String, Object> exportBlobs() {
        int totalProcessed = 0;

        // 1. Export Track cover and video blobs
        List<Track> tracks = trackRepository.findAll();
        for (Track track : tracks) {
            boolean updated = false;
            if (track.getCoverImageBlob() != null && track.getCoverImageBlob().length > 0) {
                String ext = detectImageExt(track.getCoverImageBlob());
                String key = "tracks/" + track.getId() + "/cover." + ext;
                storageService.store(key, track.getCoverImageBlob(), "image/" + ext);
                track.setCoverKey(key);
                track.setCoverImageBlob(null);
                updated = true;
                totalProcessed++;
            }
            if (track.getVideoBlob() != null && track.getVideoBlob().length > 0) {
                String key = "tracks/" + track.getId() + "/video.mp4";
                storageService.store(key, track.getVideoBlob(), "video/mp4");
                track.setStreamKey(key);
                track.setVideoBlob(null);
                updated = true;
                totalProcessed++;
            }
            if (updated) {
                trackRepository.save(track);
            }
        }

        // 2. Export Album cover blobs
        List<Album> albums = albumRepository.findAll();
        for (Album album : albums) {
            if (album.getCoverImageBlob() != null && album.getCoverImageBlob().length > 0) {
                String ext = detectImageExt(album.getCoverImageBlob());
                String key = "albums/" + album.getId() + "/cover." + ext;
                storageService.store(key, album.getCoverImageBlob(), "image/" + ext);
                album.setCoverKey(key);
                album.setCoverImageBlob(null);
                albumRepository.save(album);
                totalProcessed++;
            }
        }

        // 3. Export UserProfile avatar blobs
        List<UserProfile> profiles = userProfileRepository.findAll();
        for (UserProfile profile : profiles) {
            if (profile.getAvatarImageBlob() != null && profile.getAvatarImageBlob().length > 0) {
                String ext = detectImageExt(profile.getAvatarImageBlob());
                String key = "users/" + profile.getId() + "/avatar." + ext;
                storageService.store(key, profile.getAvatarImageBlob(), "image/" + ext);
                profile.setAvatarKey(key);
                profile.setAvatarImageBlob(null);
                userProfileRepository.save(profile);
                totalProcessed++;
            }
        }

        // 4. Export Merchandise image blobs
        List<Merchandise> merchList = merchandiseRepository.findAll();
        for (Merchandise m : merchList) {
            boolean updated = false;
            if (m.getImageBlob() != null && m.getImageBlob().length > 0) {
                String key = "merchandise/" + m.getId() + "/image_1." + detectImageExt(m.getImageBlob());
                storageService.store(key, m.getImageBlob(), "image/jpeg");
                m.setImageKey1(key);
                m.setImageBlob(null);
                updated = true;
                totalProcessed++;
            }
            if (m.getImageBlob2() != null && m.getImageBlob2().length > 0) {
                String key = "merchandise/" + m.getId() + "/image_2." + detectImageExt(m.getImageBlob2());
                storageService.store(key, m.getImageBlob2(), "image/jpeg");
                m.setImageKey2(key);
                m.setImageBlob2(null);
                updated = true;
                totalProcessed++;
            }
            if (m.getImageBlob3() != null && m.getImageBlob3().length > 0) {
                String key = "merchandise/" + m.getId() + "/image_3." + detectImageExt(m.getImageBlob3());
                storageService.store(key, m.getImageBlob3(), "image/jpeg");
                m.setImageKey3(key);
                m.setImageBlob3(null);
                updated = true;
                totalProcessed++;
            }
            if (m.getImageBlob4() != null && m.getImageBlob4().length > 0) {
                String key = "merchandise/" + m.getId() + "/image_4." + detectImageExt(m.getImageBlob4());
                storageService.store(key, m.getImageBlob4(), "image/jpeg");
                m.setImageKey4(key);
                m.setImageBlob4(null);
                updated = true;
                totalProcessed++;
            }
            if (m.getImageBlob5() != null && m.getImageBlob5().length > 0) {
                String key = "merchandise/" + m.getId() + "/image_5." + detectImageExt(m.getImageBlob5());
                storageService.store(key, m.getImageBlob5(), "image/jpeg");
                m.setImageKey5(key);
                m.setImageBlob5(null);
                updated = true;
                totalProcessed++;
            }
            if (updated) {
                merchandiseRepository.save(m);
            }
        }

        log.info("EVENT media.export_blobs processed={} total={}", totalProcessed, totalProcessed);
        return Map.of(
                "exported", totalProcessed,
                "status", "COMPLETED"
        );
    }

    private String detectImageExt(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return "bin";
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return "jpg";
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) return "png";
        if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46) return "webp";
        return "jpg";
    }
}
