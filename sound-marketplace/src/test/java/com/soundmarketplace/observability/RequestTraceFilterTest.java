package com.soundmarketplace.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    @DisplayName("Response carries generated X-Request-Id when none is provided in request")
    void testGeneratesRequestIdWhenNoneProvided() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tracks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcRequestIdDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcRequestIdDuringFilter.set(MDC.get(RequestTraceFilter.MDC_REQUEST_ID));
        };

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertThat(responseHeader).isNotBlank();
        assertThat(mdcRequestIdDuringFilter.get()).isEqualTo(responseHeader);
        assertThat(MDC.get(RequestTraceFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("Incoming X-Request-Id is propagated into MDC and response header")
    void testPropagatesIncomingRequestId() throws ServletException, IOException {
        String customId = "custom-trace-uuid-12345";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tracks");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, customId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcRequestIdDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcRequestIdDuringFilter.set(MDC.get(RequestTraceFilter.MDC_REQUEST_ID));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER)).isEqualTo(customId);
        assertThat(mdcRequestIdDuringFilter.get()).isEqualTo(customId);
        assertThat(MDC.get(RequestTraceFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("MDC is cleared cleanly even when filter chain throws exception")
    void testMdcClearedOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tracks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new RuntimeException("Simulated filter failure");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
        }

        assertThat(MDC.get(RequestTraceFilter.MDC_REQUEST_ID)).isNull();
        assertThat(MDC.get(RequestTraceFilter.MDC_USER_ID)).isNull();
    }
}
