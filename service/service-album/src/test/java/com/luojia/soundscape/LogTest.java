package com.luojia.soundscape;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@SpringBootTest
public class LogTest {

    @Test
    public void logTest() {
        log.debug("debug...");
        log.info("info...");
        log.warn("warn...");
        log.error("error...错误级别日志（无异常）");
    }

    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    public void testRedisTemplate(){
        //redisTemplate.opsForZSet().add("hot_news", "news_1", 1);
        //redisTemplate.opsForZSet().add("hot_news", "news_2", 3);
        //redisTemplate.opsForZSet().add("hot_news", "news_5", 5);
        //redisTemplate.opsForZSet().incrementScore("hot_news", "news_1", 1);
        Set set = redisTemplate.opsForZSet().reverseRangeWithScores("hot_news", 0, 2);
        for (Object o : set) {
            System.out.println(o);
        }
    }
}
