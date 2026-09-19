package com.luojia.soundscape.common.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SoundscapeCacheAspectTest {

    @Test
    void oneHundredConcurrentColdRequestsLoadOnlyOnce() throws Exception {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(null);

        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(300, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        Loader loader = new Loader();
        Method method = Loader.class.getMethod("load", Long.class);
        SoundscapeCache policy = method.getAnnotation(SoundscapeCache.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(joinPoint.getTarget()).thenReturn(loader);
        when(joinPoint.getSignature()).thenReturn(signature);

        SoundscapeCacheAspect aspect = new SoundscapeCacheAspect(redis, redisson, new NearCacheManager(),
                Runnable::run, new SimpleMeterRegistry());
        ExecutorService callers = Executors.newFixedThreadPool(100);
        CountDownLatch ready = new CountDownLatch(100);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(callers.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return aspect.around(joinPoint, policy);
                } catch (Throwable error) {
                    throw new RuntimeException(error);
                }
            }));
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<Object> future : futures) {
            assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("album-1");
        }
        callers.shutdownNow();

        assertThat(loader.loads.get()).isEqualTo(1);
        verify(lock, times(1)).tryLock(300, TimeUnit.MILLISECONDS);
    }

    static class Loader {
        private final AtomicInteger loads = new AtomicInteger();

        @SoundscapeCache(prefix = "test:")
        public String load(Long id) throws InterruptedException {
            loads.incrementAndGet();
            Thread.sleep(50);
            return "album-" + id;
        }
    }
}
