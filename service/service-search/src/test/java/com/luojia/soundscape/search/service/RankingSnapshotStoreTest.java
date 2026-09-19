package com.luojia.soundscape.search.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RankingSnapshotStoreTest {

    @Test
    void failedSnapshotWriteDoesNotReplaceActivePointer() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new RuntimeException("redis write failed")).when(hashes).putAll(anyString(), anyMap());

        RankingSnapshotStore store = new RankingSnapshotStore(redis);

        assertThatThrownBy(() -> store.publish(Map.of("1:hotScore", java.util.List.of())))
                .hasMessageContaining("redis write failed");
        verify(values, never()).set(eq("ranking:snapshot:active"), any());
    }
}
