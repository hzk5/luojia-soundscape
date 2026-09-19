package com.luojia.soundscape;

import com.luojia.soundscape.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling //开启定时任务
@Slf4j
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("服务停止...执行数据清理工作");
        }));
    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * Springboot项目启动完成后，初始化布隆过滤器
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("布隆过滤器初始化开始...");
        //1.创建布隆过滤器对象
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if (!bloomFilter.isExists()) {
            //2.初始化布隆过滤器对象 设置数据规模、误判率
            bloomFilter.tryInit(10000L, 0.03);
            log.info("布隆过滤器初始化完成...");
        }
    }


}
