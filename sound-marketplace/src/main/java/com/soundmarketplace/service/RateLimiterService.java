package com.soundmarketplace.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private final Cache<String, AtomicInteger> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public boolean tryAcquire(String endpoint, String ip, String email) {
        if (!checkLimit("endpoint:" + endpoint + ":ip:" + ip)) {
            return false;
        }
        if (email != null && !email.isBlank()) {
            if (!checkLimit("endpoint:" + endpoint + ":email:" + email.toLowerCase().trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkLimit(String key) {
        AtomicInteger counter = attemptsCache.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= 5;
    }

    public void reset() {
        attemptsCache.invalidateAll();
    }
}
