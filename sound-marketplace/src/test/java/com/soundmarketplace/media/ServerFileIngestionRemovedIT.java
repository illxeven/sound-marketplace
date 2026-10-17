package com.soundmarketplace.media;

import com.soundmarketplace.security.JwtTestTokens;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class ServerFileIngestionRemovedIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Test
    @DisplayName("CreateTrackRequest has no filePath, videoFilePath, coverImageBlob, or videoBlob fields")
    void verifyFieldsPurgedFromCreateTrackRequest() {
        List<String> fieldNames = Arrays.stream(CreateTrackRequest.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("filePath", "videoFilePath", "coverImageBlob", "videoBlob");
    }

    @Test
    @DisplayName("JSON body POST /api/tracks is rejected (returns 405 Method Not Allowed)")
    void jsonPostTracksRejected() throws Exception {
        String token = jwtTestTokens.artist(1);

        mockMvc.perform(post("/api/tracks")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Injected Track\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
