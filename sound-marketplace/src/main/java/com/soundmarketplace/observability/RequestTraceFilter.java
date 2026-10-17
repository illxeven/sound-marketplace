package com.soundmarketplace.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        QueryCountHolder.reset();

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_USER_ID, resolveUserId());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        List<String> paramNames = Collections.list(request.getParameterNames());
        log.info("HTTP_IN {} {} params={}", request.getMethod(), request.getRequestURI(), paramNames);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            MDC.put(MDC_USER_ID, resolveUserId());

            long durationMs = System.currentTimeMillis() - startTime;
            int responseBytes = responseWrapper.getContentSize();
            int sqlCount = QueryCountHolder.getCount();
            long sqlTimeMs = QueryCountHolder.getTimeMs();

            log.info("HTTP_OUT {} durationMs={} bytes={} sqlCount={} sqlTimeMs={}",
                    responseWrapper.getStatus(), durationMs, responseBytes, sqlCount, sqlTimeMs);

            responseWrapper.copyBodyToResponse();
            QueryCountHolder.clear();
            MDC.clear();
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anon";
    }
}
