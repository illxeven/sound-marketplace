package com.soundmarketplace.media;

import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class StaticHandlerRemovedIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Direct static file access to /tracks/... returns 404")
    void directTracksAccessReturns404() throws Exception {
        mockMvc.perform(get("/tracks/secret_master.wav"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Direct static file access to /uploads/... returns 404")
    void directUploadsAccessReturns404() throws Exception {
        mockMvc.perform(get("/uploads/audio/track_1.mp3"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/uploads/images/cover.jpg"))
                .andExpect(status().isNotFound());
    }
}
