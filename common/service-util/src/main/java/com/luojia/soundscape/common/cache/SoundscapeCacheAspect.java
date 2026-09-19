package com.luojia.soundscape.common.cache;

import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class SoundscapeCacheAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final NearCacheManager nearCacheManager;
    private final Executor cacheRefreshExecutor;
    private final MeterRegistry meterRegistry;
    private final Map<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();
    private final java.util.Set<String> refreshing = ConcurrentHashMap.newKeySet();
    private final Semaphore databaseFallbackPermits = new Semaphore(16);

    public SoundscapeCacheAspect(RedisTemplate<String, Object> redisTemplate,
                            RedissonClient redissonClient,
                            NearCacheManager nearCacheManager,
                            @Qualifier("cacheRefreshExecutor") Executor cacheRefreshExecutor,
                            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.nearCacheManager = nearCacheManager;
        this.cacheRefreshExecutor = cacheRefreshExecutor;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(policy)")
    public Object around(ProceedingJoinPoint joinPoint, SoundscapeCache policy) throws Throwable {
        String key = buildKey(joinPoint, policy);
        long now = System.currentTimeMillis();
        Optional<CacheEnvelope> local = nearCacheManager.getFresh(key, now);
        if (local.isPresent()) {
            metric(policy.prefix(), "l1_hit");
            return local.get().unwrap();
        }

        Invocation invocation = invocation(joinPoint);
        try {
            CacheEnvelope envelope = readL2(key, policy);
            if (envelope != null && envelope.isFresh(now)) {
                nearCacheManager.put(key, envelope, seconds(policy.l1Ttl()));
                metric(policy.prefix(), "l2_hit");
                return envelope.unwrap();
            }
            if (envelope != null && envelope.isUsable(now)) {
                nearCacheManager.put(key, envelope, seconds(policy.l1Ttl()));
                refreshAsync(key, policy, invocation);
                metric(policy.prefix(), "stale_hit");
                return envelope.unwrap();
            }
            metric(policy.prefix(), "miss");
            return loadSingleFlight(key, policy, invocation);
        } catch (Throwable error) {
            if (!isCacheFailure(error)) {
                throw error;
            }
            log.warn("Redis/Redisson不可用，尝试L1陈旧值或限流回源, key={}", key, error);
            Optional<CacheEnvelope> stale = nearCacheManager.getStale(key, System.currentTimeMillis());
            if (stale.isPresent()) {
                metric(policy.prefix(), "l1_stale_fallback");
                return stale.get().unwrap();
            }
            return loadWithDatabaseLimit(policy, invocation);
        }
    }

    private Object loadSingleFlight(String key, SoundscapeCache policy, Invocation invocation) throws Throwable {
        CompletableFuture<Object> mine = new CompletableFuture<>();
        CompletableFuture<Object> existing = inFlight.putIfAbsent(key, mine);
        if (existing != null) {
            try {
                return existing.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException error) {
                throw unwrap(error.getCause());
            } catch (TimeoutException error) {
                throw new SoundscapeException(503, "缓存重建超时");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new SoundscapeException(503, "缓存重建被中断");
            }
        }
        try {
            Object value = rebuildWithLock(key, policy, invocation);
            mine.complete(value);
            return value;
        } catch (Throwable error) {
            mine.completeExceptionally(error);
            throw error;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private Object rebuildWithLock(String key, SoundscapeCache policy, Invocation invocation) throws Throwable {
        RLock lock = redissonClient.getLock(key + RedisConstant.CACHE_LOCK_SUFFIX);
        boolean locked = false;
        try {
            locked = lock.tryLock(300, TimeUnit.MILLISECONDS);
            if (!locked) {
                CacheEnvelope winner = readL2(key, policy);
                if (winner != null && winner.isUsable(System.currentTimeMillis())) {
                    nearCacheManager.put(key, winner, seconds(policy.l1Ttl()));
                    return winner.unwrap();
                }
                metric(policy.prefix(), "rebuild_rejected");
                throw new SoundscapeException(503, "缓存正在重建，请稍后重试");
            }
            CacheEnvelope doubleChecked = readL2(key, policy);
            if (doubleChecked != null && doubleChecked.isFresh(System.currentTimeMillis())) {
                nearCacheManager.put(key, doubleChecked, seconds(policy.l1Ttl()));
                return doubleChecked.unwrap();
            }
            Object value = invocation.call();
            writeCache(key, value, policy);
            metric(policy.prefix(), "rebuild");
            return value;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void refreshAsync(String key, SoundscapeCache policy, Invocation invocation) {
        if (!refreshing.add(key)) {
            return;
        }
        try {
            cacheRefreshExecutor.execute(() -> {
                RLock lock = redissonClient.getLock(key + RedisConstant.CACHE_LOCK_SUFFIX);
                boolean locked = false;
                try {
                    locked = lock.tryLock(0, TimeUnit.MILLISECONDS);
                    if (!locked) {
                        return;
                    }
                    CacheEnvelope latest = readL2(key, policy);
                    if (latest != null && latest.isFresh(System.currentTimeMillis())) {
                        nearCacheManager.put(key, latest, seconds(policy.l1Ttl()));
                        return;
                    }
                    Object value = invocation.call();
                    writeCache(key, value, policy);
                    metric(policy.prefix(), "async_refresh");
                } catch (Throwable error) {
                    log.warn("异步刷新缓存失败, key={}", key, error);
                } finally {
                    if (locked && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    refreshing.remove(key);
                }
            });
        } catch (RejectedExecutionException error) {
            refreshing.remove(key);
            metric(policy.prefix(), "refresh_rejected");
        }
    }

    private CacheEnvelope readL2(String key, SoundscapeCache policy) {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof CacheEnvelope envelope) {
            return envelope;
        }
        long now = System.currentTimeMillis();
        long freshMillis = policy.timeUnit().toMillis(policy.ttl());
        CacheEnvelope migrated = new CacheEnvelope(raw, false, now + freshMillis,
                now + freshMillis + seconds(policy.staleTtl()));
        writeEnvelope(key, migrated);
        metric(policy.prefix(), "legacy_migrated");
        return migrated;
    }

    private void writeCache(String key, Object value, SoundscapeCache policy) {
        long now = System.currentTimeMillis();
        boolean nullValue = value == null;
        long freshMillis = nullValue ? seconds(policy.nullTtl()) : policy.timeUnit().toMillis(policy.ttl());
        long staleMillis = nullValue ? 0 : seconds(policy.staleTtl());
        CacheEnvelope envelope = new CacheEnvelope(value, nullValue, now + freshMillis,
                now + freshMillis + staleMillis);
        nearCacheManager.put(key, envelope, seconds(policy.l1Ttl()));
        try {
            writeEnvelope(key, envelope);
        } catch (RuntimeException error) {
            if (!isCacheFailure(error)) {
                throw error;
            }
            log.warn("回源成功但写入Redis失败, key={}", key, error);
        }
    }

    private void writeEnvelope(String key, CacheEnvelope envelope) {
        long remaining = Math.max(1, envelope.getStaleUntil() - System.currentTimeMillis());
        long jitterMillis = ThreadLocalRandom.current().nextLong(60_000, 600_001);
        redisTemplate.opsForValue().set(key, envelope, remaining + jitterMillis, TimeUnit.MILLISECONDS);
    }

    private Object loadWithDatabaseLimit(SoundscapeCache policy, Invocation invocation) throws Throwable {
        if (!databaseFallbackPermits.tryAcquire()) {
            metric(policy.prefix(), "database_rejected");
            throw new SoundscapeException(503, "缓存故障且数据库回源已达上限");
        }
        try {
            metric(policy.prefix(), "database_fallback");
            return invocation.call();
        } finally {
            databaseFallbackPermits.release();
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint, SoundscapeCache policy) {
        Object[] args = joinPoint.getArgs();
        String suffix = args == null || args.length == 0
                ? ((MethodSignature) joinPoint.getSignature()).getMethod().getName()
                : Arrays.stream(args).map(String::valueOf).collect(Collectors.joining("_"));
        return policy.prefix() + suffix;
    }

    private Invocation invocation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = Arrays.copyOf(joinPoint.getArgs(), joinPoint.getArgs().length);
        ReflectionUtils.makeAccessible(method);
        return () -> {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException error) {
                throw unwrap(error.getTargetException());
            }
        };
    }

    private Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private boolean isCacheFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RedisConnectionFailureException
                    || current instanceof RedisException
                    || current instanceof DataAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long seconds(long value) {
        return TimeUnit.SECONDS.toMillis(value);
    }

    private void metric(String cache, String result) {
        Counter.builder("luojia-soundscape.cache.requests")
                .tag("cache", cache)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    @FunctionalInterface
    private interface Invocation {
        Object call() throws Throwable;
    }
}
