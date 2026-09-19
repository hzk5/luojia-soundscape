package com.luojia.soundscape.search.service;

import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.model.search.AlbumInfoIndex;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RankingSnapshotStore {

    private final RedisTemplate<String, Object> redisTemplate;

    public RankingSnapshotStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(Map<String, Object> snapshot) {
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("排行榜快照不能为空");
        }
        String version = System.currentTimeMillis() + "-" + UUID.randomUUID();
        String snapshotKey = RedisConstant.RANKING_SNAPSHOT_PREFIX + version;
        redisTemplate.opsForHash().putAll(snapshotKey, snapshot);
        redisTemplate.expire(snapshotKey, 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(RedisConstant.RANKING_SNAPSHOT_ACTIVE, version);
    }

    @SuppressWarnings("unchecked")
    public List<AlbumInfoIndex> read(Long category1Id, String dimension) {
        Object activeVersion = redisTemplate.opsForValue().get(RedisConstant.RANKING_SNAPSHOT_ACTIVE);
        if (activeVersion == null) {
            return null;
        }
        Object value = redisTemplate.opsForHash().get(
                RedisConstant.RANKING_SNAPSHOT_PREFIX + activeVersion,
                category1Id + ":" + dimension);
        return value instanceof List<?> ? (List<AlbumInfoIndex>) value : null;
    }
}
