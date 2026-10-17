package com.soundmarketplace.performance;

import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class ImageCachingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrackRepository trackRepository;

    private Track trackWithPng;

    @BeforeEach
    void setUp() {
        // Valid PNG header fixture: 89 50 4E 47 0D 0A 1A 0A followed by dummy data
        byte[] pngBytes = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        trackWithPng = trackRepository.save(Track.builder()
                .title("Cached Track Image")
                .price(BigDecimal.valueOf(0.99))
                .filePath("/uploads/tracks/img_cache.mp3")
                .coverImageBlob(pngBytes)
                .build());
    }

    @Test
    @DisplayName("NTF-27: Image endpoint returns ETag, Cache-Control public 7 days, and image/png Content-Type for PNG")
    void testImageHeadersAndContentType() throws Exception {
        mockMvc.perform(get("/api/tracks/" + trackWithPng.getId() + "/cover"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "max-age=604800, public"))
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    @DisplayName("NTF-27: If-None-Match matching ETag returns 304 Not Modified with empty body")
    void testIfNoneMatchReturns304() throws Exception {
        MvcResult firstResult = mockMvc.perform(get("/api/tracks/" + trackWithPng.getId() + "/cover"))
                .andExpect(status().isOk())
                .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();

        MvcResult conditionalResult = mockMvc.perform(get("/api/tracks/" + trackWithPng.getId() + "/cover")
                .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andReturn();

        assertThat(conditionalResult.getResponse().getContentLength()).isZero();
    }
}
