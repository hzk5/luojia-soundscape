package com.luojia.soundscape.search.config;

import com.luojia.soundscape.common.zipkin.ZipkinHelper;
import com.luojia.soundscape.common.zipkin.ZipkinTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SearchExecutorConfig {

    @Bean("searchIndexExecutor")
    public ThreadPoolTaskExecutor searchIndexExecutor(ZipkinHelper zipkinHelper) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("search-index-");
        executor.setTaskDecorator(new ZipkinTaskDecorator(zipkinHelper));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
