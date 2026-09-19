package com.luojia.soundscape.common.cache;

import com.luojia.soundscape.common.constant.RedisConstant;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SoundscapeCache {


    /**
     * 缓存到Redis的前缀（业务数据或锁）
     * @return
     */
    String prefix() default "";

    /**
     * 缓存时间
     * @return
     */
    long ttl() default 600;

    /**
     * 时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    long staleTtl() default 300;

    long l1Ttl() default 5;

    long nullTtl() default 60;

}
