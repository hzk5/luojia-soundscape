package com.luojia.soundscape.common.rabbit.service;

import com.luojia.soundscape.common.rabbit.entity.CacheInvalidationMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {

    private final CacheInvalidationService invalidationService;

    public CacheInvalidationListener(CacheInvalidationService invalidationService) {
        this.invalidationService = invalidationService;
    }

    @RabbitListener(queues = "#{cacheInvalidationQueue.name}")
    public void invalidate(CacheInvalidationMessage message) {
        if (message != null && message.getKey() != null) {
            invalidationService.evictNow(message.getKey());
        }
    }
}
