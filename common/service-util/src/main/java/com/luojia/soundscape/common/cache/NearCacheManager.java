package com.luojia.soundscape.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NearCacheManager {
    private final Cache<String, LocalEntry> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    public Optional<CacheEnvelope> getFresh(String key, long now) {
        LocalEntry entry = cache.getIfPresent(key);
        if (entry == null || now >= entry.l1FreshUntil()) {
            return Optional.empty();
        }
        return Optional.of(entry.envelope());
    }

    public Optional<CacheEnvelope> getStale(String key, long now) {
        LocalEntry entry = cache.getIfPresent(key);
        if (entry == null || !entry.envelope().isUsable(now)) {
            return Optional.empty();
        }
        return Optional.of(entry.envelope());
    }

    public void put(String key, CacheEnvelope envelope, long l1TtlMillis) {
        cache.put(key, new LocalEntry(envelope, System.currentTimeMillis() + l1TtlMillis));
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    private record LocalEntry(CacheEnvelope envelope, long l1FreshUntil) {
    }
}
