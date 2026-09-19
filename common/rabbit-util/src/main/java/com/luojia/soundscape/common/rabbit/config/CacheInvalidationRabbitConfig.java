package com.luojia.soundscape.common.rabbit.config;

import com.luojia.soundscape.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CacheInvalidationRabbitConfig {

    @Bean
    public CustomExchange cacheInvalidationExchange() {
        return new CustomExchange(MqConst.EXCHANGE_CACHE_INVALIDATE, "x-delayed-message",
                true, false, Map.of("x-delayed-type", "fanout"));
    }

    @Bean
    public AnonymousQueue cacheInvalidationQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding cacheInvalidationBinding(AnonymousQueue cacheInvalidationQueue,
                                             CustomExchange cacheInvalidationExchange) {
        return BindingBuilder.bind(cacheInvalidationQueue).to(cacheInvalidationExchange).with("").noargs();
    }
}
