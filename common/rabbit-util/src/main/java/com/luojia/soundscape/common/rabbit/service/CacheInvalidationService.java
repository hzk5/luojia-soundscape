package com.luojia.soundscape.common.rabbit.service;

import com.luojia.soundscape.common.cache.NearCacheManager;
import com.luojia.soundscape.common.rabbit.constant.MqConst;
import com.luojia.soundscape.common.rabbit.entity.CacheInvalidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NearCacheManager nearCacheManager;
    private final RabbitTemplate rabbitTemplate;

    public CacheInvalidationService(RedisTemplate<String, Object> redisTemplate,
                                    NearCacheManager nearCacheManager,
                                    RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.nearCacheManager = nearCacheManager;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void evictAfterCommit(String key) {
        Runnable action = () -> publishInvalidation(key);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    public void evictNow(String key) {
        nearCacheManager.evict(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException error) {
            log.warn("删除Redis缓存失败, key={}", key, error);
        }
    }

    private void publishInvalidation(String key) {
        evictNow(key);
        try {
            CacheInvalidationMessage message = new CacheInvalidationMessage(key);
            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CACHE_INVALIDATE, "", message);
            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CACHE_INVALIDATE, "", message, rabbitMessage -> {
                rabbitMessage.getMessageProperties().setDelay(1000);
                return rabbitMessage;
            });
        } catch (RuntimeException error) {
            log.warn("发布缓存失效事件失败, key={}", key, error);
        }
    }
}
