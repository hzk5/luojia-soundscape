package com.luojia.soundscape.common.config.thread;

import com.luojia.soundscape.common.zipkin.ZipkinHelper;
import com.luojia.soundscape.common.zipkin.ZipkinTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadConfig {

    @Bean("cacheRefreshExecutor")
    public ThreadPoolTaskExecutor cacheRefreshExecutor(ZipkinHelper zipkinHelper) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("cache-refresh-");
        executor.setTaskDecorator(new ZipkinTaskDecorator(zipkinHelper));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
