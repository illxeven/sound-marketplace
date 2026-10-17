package com.soundmarketplace.security;

import com.soundmarketplace.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class ErrorHandlingIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private CatalogService catalogService;

    @Test
    @DisplayName("NTF-21: Unknown entity returns 404 Not Found")
    void unknownEntityReturns404() throws Exception {
        mockMvc.perform(get("/api/tracks/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("NTF-21: Unhandled internal exception returns generic 500 without leaking sensitive error or stack trace")
    void unhandledExceptionReturnsGeneric500() throws Exception {
        Mockito.doThrow(new RuntimeException("SECRET_SQL_INJECTION_OR_INTERNAL_FAILURE"))
                .when(catalogService).getAllTracks();

        mockMvc.perform(get("/api/tracks"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.message").value(not(containsString("SECRET_SQL_INJECTION_OR_INTERNAL_FAILURE"))));
    }
}
